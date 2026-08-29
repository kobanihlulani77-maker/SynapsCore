package com.synapsecore.integration;

import com.synapsecore.access.SynapseActorContext;
import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.dto.OrderItemRequest;
import com.synapsecore.domain.dto.OrderResponse;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.integration.IntegrationConnectorPolicyService.PreparedConnectorOrder;
import com.synapsecore.integration.dto.ExternalOrderWebhookRequest;
import com.synapsecore.integration.dto.ExternalOrderWebhookResponse;
import com.synapsecore.observability.OperationalMetricsService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalOrderWebhookService {

    private final com.synapsecore.domain.service.OrderService orderService;
    private final IntegrationConnectorService integrationConnectorService;
    private final IntegrationConnectorPolicyService integrationConnectorPolicyService;
    private final IntegrationImportRunService integrationImportRunService;
    private final IntegrationReplayService integrationReplayService;
    private final IntegrationInboundRecordService integrationInboundRecordService;
    private final RequestTraceContext requestTraceContext;
    private final OperationalMetricsService operationalMetricsService;

    public ExternalOrderWebhookResponse ingest(ExternalOrderWebhookRequest request) {
        return ingest(request, null);
    }

    public ExternalOrderWebhookResponse ingest(ExternalOrderWebhookRequest request,
                                               com.synapsecore.domain.entity.IntegrationConnector authenticatedConnector) {
        return ingest(request, authenticatedConnector, null);
    }

    public ExternalOrderWebhookResponse ingest(ExternalOrderWebhookRequest request,
                                               com.synapsecore.domain.entity.IntegrationConnector authenticatedConnector,
                                               SynapseActorContext humanActor) {
        String sourceSystem = request.sourceSystem().trim().toLowerCase(Locale.ROOT);
        OrderCreateRequest mappedRequest = null;
        Long inboundRecordId = null;
        OrderResponse createdOrder = null;
        String tenantCode = requestTraceContext.getCurrentTenant()
            .filter(currentTenant -> !RequestTraceContext.MISSING_TENANT_CONTEXT.equalsIgnoreCase(currentTenant))
            .orElse(authenticatedConnector != null ? integrationConnectorService.resolveTenantCode(authenticatedConnector) : null);

        try {
            var connector = authenticatedConnector != null
                ? authenticatedConnector
                : integrationConnectorService.requireEnabledConnector(
                    sourceSystem,
                    IntegrationConnectorType.WEBHOOK_ORDER,
                    "accept webhook orders");
            tenantCode = integrationConnectorService.resolveTenantCode(connector);
            sourceSystem = connector.getSourceSystem();
            inboundRecordId = integrationInboundRecordService.recordReceived(
                tenantCode,
                sourceSystem,
                IntegrationConnectorType.WEBHOOK_ORDER,
                null,
                request.externalOrderId() == null ? null : request.externalOrderId().trim(),
                request.warehouseCode() == null ? null : request.warehouseCode().trim(),
                request
            ).getId();
            PreparedConnectorOrder preparedOrder = integrationConnectorPolicyService.prepareOrder(
                connector,
                request.externalOrderId(),
                request.warehouseCode(),
                request.customerReference(),
                request.occurredAt(),
                mapItems(request)
            );
            mappedRequest = preparedOrder.orderRequest();
            requireHumanWarehouseAccess(humanActor, mappedRequest.warehouseCode());
            String ingestionSource = "integration-webhook:" + sourceSystem.toLowerCase(Locale.ROOT);

            createdOrder = orderService.createOrder(mappedRequest, ingestionSource);
            integrationInboundRecordService.markAccepted(inboundRecordId, ingestionSource);
            integrationImportRunService.recordRun(
                sourceSystem,
                IntegrationConnectorType.WEBHOOK_ORDER,
                null,
                1,
                1,
                0,
                "Accepted webhook order " + createdOrder.externalOrderId() + " from " + sourceSystem + "."
            );

            return new ExternalOrderWebhookResponse(
                sourceSystem,
                ingestionSource,
                Instant.now(),
                createdOrder
            );
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            var failure = IntegrationFailureCodes.extract(exception);
            operationalMetricsService.recordIntegrationFailure(tenantCode, sourceSystem, failure.failureCode().name());
            log.warn("Webhook ingestion failed for source {} tenant {} externalOrderId {}: {}",
                sourceSystem,
                tenantCode,
                request.externalOrderId(),
                failure.failureMessage());
            integrationInboundRecordService.markRejected(inboundRecordId, failure.failureCode(), failure.failureMessage());
            integrationImportRunService.recordRun(
                sourceSystem,
                IntegrationConnectorType.WEBHOOK_ORDER,
                null,
                1,
                0,
                1,
                "Rejected webhook order " + (mappedRequest == null ? normalizeExternalOrderId(request.externalOrderId()) : mappedRequest.externalOrderId()) + " from " + sourceSystem
                    + ". Reason: " + failure.failureMessage()
            );
            if (IntegrationFailureCodes.isReplayable(failure.failureCode())) {
                integrationReplayService.recordFailure(
                    tenantCode,
                    sourceSystem,
                    IntegrationConnectorType.WEBHOOK_ORDER,
                    mappedRequest == null
                        ? new OrderCreateRequest(
                            request.externalOrderId().trim(),
                            request.warehouseCode() == null ? "" : request.warehouseCode().trim(),
                            mapItems(request)
                        )
                        : mappedRequest,
                    failure.failureCode(),
                    failure.failureMessage(),
                    inboundRecordId
                );
            }
            throw exception;
        } catch (RuntimeException exception) {
            String failureMessage = "Webhook processing failed unexpectedly; operator reconciliation is required.";
            operationalMetricsService.recordIntegrationFailure(tenantCode, sourceSystem, IntegrationFailureCode.UNKNOWN.name());
            if (inboundRecordId != null) {
                if (createdOrder == null) {
                    try {
                        integrationInboundRecordService.markRejected(inboundRecordId, IntegrationFailureCode.UNKNOWN, failureMessage);
                    } catch (RuntimeException evidenceException) {
                        log.error("Webhook rejection evidence could not be finalized for inbound record {}.", inboundRecordId,
                            evidenceException);
                    }
                } else {
                    try {
                        integrationInboundRecordService.markAccepted(inboundRecordId,
                            "integration-webhook:" + sourceSystem.toLowerCase(Locale.ROOT));
                    } catch (RuntimeException evidenceException) {
                        log.error("Accepted webhook evidence could not be finalized for inbound record {} after order {} was created.",
                            inboundRecordId, createdOrder.externalOrderId(), evidenceException);
                    }
                }
            }
            log.error("Webhook ingestion failed unexpectedly for source {} tenant {} externalOrderId {}.",
                sourceSystem, tenantCode, request.externalOrderId(), exception);
            throw exception;
        }
    }

    private void requireHumanWarehouseAccess(SynapseActorContext humanActor, String warehouseCode) {
        if (humanActor != null && !humanActor.canAccessWarehouse(warehouseCode)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Current actor " + humanActor.actorName()
                    + " is not assigned to warehouse " + warehouseCode + "."
            );
        }
    }

    private List<OrderItemRequest> mapItems(ExternalOrderWebhookRequest request) {
        return request.items().stream()
            .map(item -> new OrderItemRequest(
                item.productSku().trim(),
                item.quantity(),
                item.unitPrice()
            ))
            .toList();
    }

    private String normalizeExternalOrderId(String externalOrderId) {
        return externalOrderId == null ? "<missing>" : externalOrderId.trim();
    }
}
