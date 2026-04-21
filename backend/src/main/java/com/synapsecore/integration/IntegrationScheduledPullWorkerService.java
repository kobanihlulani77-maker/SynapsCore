package com.synapsecore.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.entity.IntegrationConnector;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationSyncMode;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.integration.dto.ExternalOrderWebhookRequest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationScheduledPullWorkerService {

    private static final String WORKER_ACTOR = "integration-pull-worker";
    private static final String RUN_FILE_NAME = "scheduled-pull";

    private final IntegrationConnectorRepository integrationConnectorRepository;
    private final ExternalOrderWebhookService externalOrderWebhookService;
    private final IntegrationImportRunService integrationImportRunService;
    private final ObjectMapper objectMapper;
    private final RequestTraceContext requestTraceContext;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Value("${synapsecore.integration.pull-worker.enabled:true}")
    private boolean workerEnabled;

    @Value("${synapsecore.integration.pull-worker.batch-size:10}")
    private int workerBatchSize;

    @Value("${synapsecore.integration.pull-worker.fetch-timeout-seconds:20}")
    private int fetchTimeoutSeconds;

    @Scheduled(fixedDelayString = "${synapsecore.integration.pull-worker.interval-ms:60000}")
    public void processScheduledPulls() {
        if (!workerEnabled) {
            return;
        }
        int processed = processDuePulls(Math.max(workerBatchSize, 1));
        if (processed > 0) {
            log.info("Processed {} scheduled integration pull connector(s).", processed);
        }
    }

    public int processDuePulls(int maxConnectors) {
        List<IntegrationConnector> dueConnectors = integrationConnectorRepository
            .findAllEnabledBySyncModeWithTenant(IntegrationSyncMode.SCHEDULED_PULL)
            .stream()
            .filter(this::isSupportedPullConnector)
            .filter(this::isDue)
            .limit(Math.max(maxConnectors, 1))
            .toList();

        dueConnectors.forEach(this::processConnector);
        return dueConnectors.size();
    }

    private boolean isSupportedPullConnector(IntegrationConnector connector) {
        return connector.getType() == IntegrationConnectorType.WEBHOOK_ORDER
            && connector.getPullEndpointUrl() != null
            && !connector.getPullEndpointUrl().isBlank();
    }

    private boolean isDue(IntegrationConnector connector) {
        if (connector.getSyncIntervalMinutes() == null || connector.getSyncIntervalMinutes() < 15) {
            return true;
        }
        if (connector.getLastPullAttemptAt() == null) {
            return true;
        }
        return !connector.getLastPullAttemptAt()
            .plus(Duration.ofMinutes(connector.getSyncIntervalMinutes()))
            .isAfter(Instant.now());
    }

    private void processConnector(IntegrationConnector connector) {
        markPullAttempt(connector);
        try {
            String responseBody = fetchConnectorPayload(connector);
            List<ExternalOrderWebhookRequest> orders = parseOrders(responseBody, connector);
            int imported = 0;
            int failed = 0;
            for (ExternalOrderWebhookRequest order : orders) {
                if (ingestOrder(connector, order)) {
                    imported++;
                } else {
                    failed++;
                }
            }
            markPullComplete(connector, imported, failed, orders.size());
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            markPullFailed(connector, exception);
            recordPullFailure(connector, exception);
        }
    }

    private String fetchConnectorPayload(IntegrationConnector connector) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(connector.getPullEndpointUrl().trim()))
            .GET()
            .timeout(Duration.ofSeconds(Math.max(fetchTimeoutSeconds, 1)))
            .header("Accept", "application/json")
            .header("X-SynapseCore-Tenant", connector.getTenant().getCode())
            .header("X-SynapseCore-Connector", connector.getSourceSystem())
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Scheduled pull returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    private List<ExternalOrderWebhookRequest> parseOrders(String responseBody,
                                                          IntegrationConnector connector) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode orderNode = root.isArray()
            ? root
            : root.has("orders") ? root.get("orders") : root;

        List<ExternalOrderWebhookRequest> orders = new ArrayList<>();
        if (orderNode.isArray()) {
            for (JsonNode item : orderNode) {
                orders.add(normalizeOrder(objectMapper.treeToValue(item, ExternalOrderWebhookRequest.class), connector));
            }
        } else if (!orderNode.isMissingNode() && !orderNode.isNull()) {
            orders.add(normalizeOrder(objectMapper.treeToValue(orderNode, ExternalOrderWebhookRequest.class), connector));
        }
        return orders;
    }

    private ExternalOrderWebhookRequest normalizeOrder(ExternalOrderWebhookRequest order,
                                                       IntegrationConnector connector) {
        return new ExternalOrderWebhookRequest(
            connector.getSourceSystem(),
            order.externalOrderId(),
            order.warehouseCode(),
            order.customerReference(),
            order.occurredAt(),
            order.items()
        );
    }

    private boolean ingestOrder(IntegrationConnector connector,
                                ExternalOrderWebhookRequest order) {
        try {
            requestTraceContext.setCurrentRequestId("scheduled-pull-" + connector.getId() + "-" + Instant.now().toEpochMilli());
            requestTraceContext.setCurrentActor(WORKER_ACTOR);
            requestTraceContext.setCurrentTenant(connector.getTenant().getCode());
            externalOrderWebhookService.ingest(order, connector);
            return true;
        } catch (ResponseStatusException exception) {
            log.warn("Scheduled pull order from {} was rejected: {}", connector.getSourceSystem(), exception.getReason());
            return false;
        } catch (RuntimeException exception) {
            log.warn("Scheduled pull order from {} failed unexpectedly.", connector.getSourceSystem(), exception);
            return false;
        } finally {
            requestTraceContext.clear();
        }
    }

    private void markPullAttempt(IntegrationConnector connector) {
        connector.setLastPullAttemptAt(Instant.now());
        connector.setLastPullStatus("RUNNING");
        connector.setLastPullMessage("Scheduled pull started.");
        integrationConnectorRepository.save(connector);
    }

    private void markPullComplete(IntegrationConnector connector,
                                  int imported,
                                  int failed,
                                  int received) {
        connector.setLastPullSuccessAt(failed == 0 ? Instant.now() : connector.getLastPullSuccessAt());
        connector.setLastPullStatus(failed == 0 ? "SUCCESS" : imported > 0 ? "PARTIAL_SUCCESS" : "FAILURE");
        connector.setLastPullMessage("Scheduled pull received " + received + " order(s), imported " + imported
            + ", failed " + failed + ".");
        integrationConnectorRepository.save(connector);
    }

    private void markPullFailed(IntegrationConnector connector, Exception exception) {
        connector.setLastPullStatus("FAILURE");
        connector.setLastPullMessage(truncate("Scheduled pull failed: " + exception.getMessage(), 512));
        integrationConnectorRepository.save(connector);
    }

    private void recordPullFailure(IntegrationConnector connector, Exception exception) {
        try {
            requestTraceContext.setCurrentRequestId("scheduled-pull-" + connector.getId() + "-" + Instant.now().toEpochMilli());
            requestTraceContext.setCurrentActor(WORKER_ACTOR);
            requestTraceContext.setCurrentTenant(connector.getTenant().getCode());
            integrationImportRunService.recordRun(
                connector.getSourceSystem(),
                connector.getType(),
                RUN_FILE_NAME,
                1,
                0,
                1,
                truncate("Scheduled pull failed before ingestion: " + exception.getMessage(), 320)
            );
        } finally {
            requestTraceContext.clear();
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
