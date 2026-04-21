package com.synapsecore.integration;

import com.synapsecore.access.AccessDirectoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.audit.AuditLogService;
import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.dto.OrderResponse;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.domain.entity.IntegrationReplayRecord;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.service.OrderService;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.event.OperationalStateChangePublisher;
import com.synapsecore.event.OperationalUpdateType;
import com.synapsecore.integration.dto.IntegrationReplayRecordResponse;
import com.synapsecore.integration.dto.IntegrationReplayResultResponse;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.tenant.TenantContextService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class IntegrationReplayService {

    private static final int DEFAULT_QUEUE_LIMIT = 12;
    private static final String AUTO_REPLAY_ACTOR = "system-replay";

    private final IntegrationReplayRecordRepository integrationReplayRecordRepository;
    private final AccessDirectoryService accessDirectoryService;
    private final IntegrationConnectorService integrationConnectorService;
    private final OrderService orderService;
    private final BusinessEventService businessEventService;
    private final AuditLogService auditLogService;
    private final OperationalStateChangePublisher operationalStateChangePublisher;
    private final ObjectMapper objectMapper;
    private final TenantContextService tenantContextService;
    private final OperationalMetricsService operationalMetricsService;
    private final IntegrationInboundRecordService integrationInboundRecordService;
    private final RequestTraceContext requestTraceContext;

    @Value("${synapsecore.integration.replay.max-attempts:3}")
    private int maxReplayAttempts;

    @Value("${synapsecore.integration.replay.backoff-seconds:300}")
    private long replayBackoffSeconds;

    @Transactional
    public IntegrationReplayRecordResponse recordFailure(String sourceSystem,
                                                         IntegrationConnectorType connectorType,
                                                         OrderCreateRequest request,
                                                         String failureMessage) {
        return recordFailure(
            tenantContextService.getCurrentTenantCodeOrDefault(),
            sourceSystem,
            connectorType,
            request,
            IntegrationFailureCode.UNKNOWN,
            failureMessage,
            null
        );
    }

    @Transactional
    public IntegrationReplayRecordResponse recordFailure(String tenantCode,
                                                         String sourceSystem,
                                                         IntegrationConnectorType connectorType,
                                                         OrderCreateRequest request,
                                                         IntegrationFailureCode failureCode,
                                                         String failureMessage,
                                                         Long inboundRecordId) {
        IntegrationReplayRecord record = integrationReplayRecordRepository.save(IntegrationReplayRecord.builder()
            .tenantCode(tenantCode)
            .sourceSystem(sourceSystem)
            .connectorType(connectorType)
            .externalOrderId(request.externalOrderId())
            .warehouseCode(request.warehouseCode())
            .requestPayload(serializeRequest(request))
            .failureCode(failureCode)
            .failureMessage(limit(failureMessage))
            .status(IntegrationReplayStatus.PENDING)
            .replayAttemptCount(0)
            .inboundRecordId(inboundRecordId)
            .nextEligibleAt(Instant.now())
            .build());
        integrationInboundRecordService.markReplayQueued(inboundRecordId, record.getId(), failureCode, failureMessage);

        businessEventService.record(
            BusinessEventType.INTEGRATION_REPLAY_QUEUED,
            "integration-replay",
            "Queued failed " + connectorType + " order " + request.externalOrderId() + " from " + sourceSystem
                + " for replay. Reason: " + failureMessage
        );
        auditLogService.recordFailure(
            "INTEGRATION_REPLAY_QUEUED",
            "integration-replay",
            "integration-replay",
            "IntegrationReplayRecord",
            String.valueOf(record.getId()),
            "Queued failed inbound order " + request.externalOrderId() + " from " + sourceSystem + "."
        );
        operationalStateChangePublisher.publish(OperationalUpdateType.INTEGRATION_STATE, "integration-replay");
        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public List<IntegrationReplayRecordResponse> getReplayQueue() {
        var currentOperator = accessDirectoryService.getCurrentOperator();
        return integrationReplayRecordRepository.findQueueSummariesByTenantCodeIgnoreCaseAndStatusIn(
                tenantContextService.getCurrentTenantCodeOrDefault(),
                List.of(IntegrationReplayStatus.PENDING, IntegrationReplayStatus.REPLAY_FAILED, IntegrationReplayStatus.DEAD_LETTERED),
                PageRequest.of(0, DEFAULT_QUEUE_LIMIT))
            .stream()
            .filter(record -> currentOperator.isEmpty()
                || accessDirectoryService.hasWarehouseAccess(currentOperator.get(), record.warehouseCode()))
            .toList();
    }

    @Transactional
    public IntegrationReplayResultResponse replay(Long replayRecordId, String actorName) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        IntegrationReplayRecord record = integrationReplayRecordRepository.findByTenantCodeIgnoreCaseAndId(
                tenantCode,
                replayRecordId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Integration replay record not found: " + replayRecordId));
        accessDirectoryService.requireOperatorWarehouseAccess(
            actorName,
            tenantCode,
            record.getWarehouseCode(),
            "replay failed inbound orders for warehouse " + record.getWarehouseCode()
        );

        if (record.getStatus() == IntegrationReplayStatus.REPLAYED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Integration replay record " + replayRecordId + " has already been resolved.");
        }
        if (record.getStatus() == IntegrationReplayStatus.DEAD_LETTERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Integration replay record " + replayRecordId + " is dead-lettered and must be re-ingested manually.");
        }

        OrderCreateRequest request = deserializeRequest(record);
        Instant attemptedAt = Instant.now();
        if (record.getNextEligibleAt() != null && record.getNextEligibleAt().isAfter(attemptedAt)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Integration replay record " + replayRecordId + " is not eligible for replay until " + record.getNextEligibleAt() + ".");
        }

        return replayRecord(record, request, attemptedAt, actorName, true);
    }

    @Transactional
    public int processAutomatedReplayBatch(int batchSize) {
        Instant now = Instant.now();
        return integrationReplayRecordRepository.findEligibleForAutomatedReplay(
                List.of(IntegrationReplayStatus.PENDING, IntegrationReplayStatus.REPLAY_FAILED),
                now,
                PageRequest.of(0, Math.max(batchSize, 1)))
            .stream()
            .mapToInt(record -> attemptAutomatedReplay(record, now))
            .sum();
    }

    private int attemptAutomatedReplay(IntegrationReplayRecord record, Instant attemptedAt) {
        OrderCreateRequest request = deserializeRequest(record);
        String previousRequestId = requestTraceContext.getCurrentRequestId().orElse(null);
        String previousActor = requestTraceContext.getCurrentActor().orElse(null);
        String previousTenant = requestTraceContext.getCurrentTenant().orElse(null);
        try {
            requestTraceContext.setCurrentRequestId("auto-replay-" + record.getId());
            requestTraceContext.setCurrentActor(AUTO_REPLAY_ACTOR);
            requestTraceContext.setCurrentTenant(record.getTenantCode());
            replayRecord(record, request, attemptedAt, AUTO_REPLAY_ACTOR, false);
            return 1;
        } catch (ResponseStatusException exception) {
            return 0;
        } finally {
            requestTraceContext.clear();
            restoreTraceValue(previousRequestId, requestTraceContext::setCurrentRequestId);
            restoreTraceValue(previousActor, requestTraceContext::setCurrentActor);
            restoreTraceValue(previousTenant, requestTraceContext::setCurrentTenant);
        }
    }

    private IntegrationReplayResultResponse replayRecord(IntegrationReplayRecord record,
                                                         OrderCreateRequest request,
                                                         Instant attemptedAt,
                                                         String actorName,
                                                         boolean enforceWarehouseAccess) {
        String tenantCode = record.getTenantCode();
        if (enforceWarehouseAccess) {
            accessDirectoryService.requireOperatorWarehouseAccess(
                actorName,
                tenantCode,
                record.getWarehouseCode(),
                "replay failed inbound orders for warehouse " + record.getWarehouseCode()
            );
        }

        try {
            integrationConnectorService.requireEnabledConnectorForTenant(
                tenantCode,
                record.getSourceSystem(),
                record.getConnectorType(),
                "replay failed inbound orders");

            OrderResponse order = orderService.createOrderForTenant(
                tenantCode,
                request,
                (enforceWarehouseAccess ? "integration-replay:" : "integration-replay:auto:") + record.getSourceSystem()
            );

            record.setStatus(IntegrationReplayStatus.REPLAYED);
            record.setReplayAttemptCount(record.getReplayAttemptCount() + 1);
            record.setLastAttemptedAt(attemptedAt);
            record.setNextEligibleAt(null);
            record.setResolvedAt(attemptedAt);
            record.setReplayedOrderExternalId(order.externalOrderId());
            record.setFailureCode(null);
            record.setLastReplayMessage(limit("Replayed successfully as live order " + order.externalOrderId() + "."));
            record = integrationReplayRecordRepository.save(record);
            integrationInboundRecordService.markReplayed(record.getInboundRecordId(), order.externalOrderId());

            businessEventService.record(
                BusinessEventType.INTEGRATION_REPLAY_COMPLETED,
                "integration-replay",
                "Replayed failed " + record.getConnectorType() + " order " + record.getExternalOrderId()
                    + " from " + record.getSourceSystem() + " by " + actorName + "."
            );
            auditLogService.recordSuccess(
                "INTEGRATION_REPLAY_COMPLETED",
                actorName,
                "integration-replay",
                "IntegrationReplayRecord",
                String.valueOf(record.getId()),
                "Replayed failed inbound order " + record.getExternalOrderId() + " successfully."
            );
            operationalMetricsService.recordReplayAttempt(tenantCode, true);
            operationalStateChangePublisher.publish(OperationalUpdateType.INTEGRATION_STATE, "integration-replay");
            return new IntegrationReplayResultResponse(toResponse(record), order, attemptedAt);
        } catch (ResponseStatusException exception) {
            var failure = IntegrationFailureCodes.extract(exception);
            int nextAttemptCount = record.getReplayAttemptCount() + 1;
            record.setReplayAttemptCount(nextAttemptCount);
            record.setLastAttemptedAt(attemptedAt);
            record.setFailureCode(failure.failureCode());
            boolean exhausted = nextAttemptCount >= Math.max(maxReplayAttempts, 1);
            if (exhausted) {
                record.setStatus(IntegrationReplayStatus.DEAD_LETTERED);
                record.setDeadLetteredAt(attemptedAt);
                record.setNextEligibleAt(null);
                record.setLastReplayMessage(limit(failure.failureMessage() + " Dead-lettered after " + nextAttemptCount + " attempts."));
            } else {
                record.setStatus(IntegrationReplayStatus.REPLAY_FAILED);
                record.setNextEligibleAt(nextEligibleAt(attemptedAt, nextAttemptCount));
                record.setLastReplayMessage(limit(failure.failureMessage()));
            }
            record = integrationReplayRecordRepository.save(record);

            businessEventService.record(
                BusinessEventType.INTEGRATION_REPLAY_FAILED,
                "integration-replay",
                "Replay failed for " + record.getExternalOrderId() + " from " + record.getSourceSystem()
                    + " by " + actorName + ". Reason: " + failure.failureMessage()
            );
            auditLogService.recordFailure(
                "INTEGRATION_REPLAY_FAILED",
                actorName,
                "integration-replay",
                "IntegrationReplayRecord",
                String.valueOf(record.getId()),
                "Replay failed for inbound order " + record.getExternalOrderId() + ". Reason: "
                    + failure.failureMessage()
            );
            operationalMetricsService.recordReplayAttempt(tenantCode, false);
            operationalStateChangePublisher.publish(OperationalUpdateType.INTEGRATION_STATE, "integration-replay");
            throw exception;
        }
    }

    private void restoreTraceValue(String value, java.util.function.Consumer<String> setter) {
        if (value != null && !value.isBlank()) {
            setter.accept(value);
        }
    }

    private OrderCreateRequest deserializeRequest(IntegrationReplayRecord record) {
        try {
            return objectMapper.readValue(record.getRequestPayload(), OrderCreateRequest.class);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Stored integration replay payload could not be read for record " + record.getId(), exception);
        }
    }

    private String serializeRequest(OrderCreateRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Integration replay payload could not be stored.", exception);
        }
    }

    private IntegrationReplayRecordResponse toResponse(IntegrationReplayRecord record) {
        return new IntegrationReplayRecordResponse(
            record.getId(),
            record.getSourceSystem(),
            record.getConnectorType(),
            record.getExternalOrderId(),
            record.getWarehouseCode(),
            record.getFailureCode(),
            record.getFailureMessage(),
            record.getStatus(),
            record.getReplayAttemptCount(),
            record.getLastReplayMessage(),
            record.getLastAttemptedAt(),
            record.getNextEligibleAt(),
            record.getResolvedAt(),
            record.getDeadLetteredAt(),
            record.getReplayedOrderExternalId(),
            record.getCreatedAt(),
            record.getUpdatedAt()
        );
    }

    private Instant nextEligibleAt(Instant attemptedAt, int attemptCount) {
        long backoffSeconds = Math.max(replayBackoffSeconds, 0L);
        if (backoffSeconds == 0L) {
            return attemptedAt;
        }
        return attemptedAt.plusSeconds(backoffSeconds * Math.max(attemptCount, 1));
    }

    private String limit(String value) {
        if (value == null || value.length() <= 320) {
            return value;
        }
        return value.substring(0, 317) + "...";
    }
}
