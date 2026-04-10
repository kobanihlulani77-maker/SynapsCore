package com.synapsecore.integration;

import com.synapsecore.access.AccessDirectoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.audit.AuditLogService;
import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.dto.OrderResponse;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationReplayRecord;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class IntegrationReplayService {

    private static final int DEFAULT_QUEUE_LIMIT = 12;

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

    @Transactional
    public IntegrationReplayRecordResponse recordFailure(String sourceSystem,
                                                         IntegrationConnectorType connectorType,
                                                         OrderCreateRequest request,
                                                         String failureMessage) {
        IntegrationReplayRecord record = integrationReplayRecordRepository.save(IntegrationReplayRecord.builder()
            .tenantCode(tenantContextService.getCurrentTenantCodeOrDefault())
            .sourceSystem(sourceSystem)
            .connectorType(connectorType)
            .externalOrderId(request.externalOrderId())
            .warehouseCode(request.warehouseCode())
            .requestPayload(serializeRequest(request))
            .failureMessage(limit(failureMessage))
            .status(IntegrationReplayStatus.PENDING)
            .replayAttemptCount(0)
            .build());

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
                List.of(IntegrationReplayStatus.PENDING, IntegrationReplayStatus.REPLAY_FAILED),
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

        OrderCreateRequest request = deserializeRequest(record);
        Instant attemptedAt = Instant.now();

        try {
            integrationConnectorService.requireEnabledConnector(
                record.getSourceSystem(),
                record.getConnectorType(),
                "replay failed inbound orders");

            OrderResponse order = orderService.createOrder(
                request,
                "integration-replay:" + record.getSourceSystem()
            );

            record.setStatus(IntegrationReplayStatus.REPLAYED);
            record.setReplayAttemptCount(record.getReplayAttemptCount() + 1);
            record.setLastAttemptedAt(attemptedAt);
            record.setResolvedAt(attemptedAt);
            record.setReplayedOrderExternalId(order.externalOrderId());
            record.setLastReplayMessage(limit("Replayed successfully as live order " + order.externalOrderId() + "."));
            record = integrationReplayRecordRepository.save(record);

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
            record.setStatus(IntegrationReplayStatus.REPLAY_FAILED);
            record.setReplayAttemptCount(record.getReplayAttemptCount() + 1);
            record.setLastAttemptedAt(attemptedAt);
            record.setLastReplayMessage(limit(exception.getReason()));
            record = integrationReplayRecordRepository.save(record);

            businessEventService.record(
                BusinessEventType.INTEGRATION_REPLAY_FAILED,
                "integration-replay",
                "Replay failed for " + record.getExternalOrderId() + " from " + record.getSourceSystem()
                    + " by " + actorName + ". Reason: " + exception.getReason()
            );
            auditLogService.recordFailure(
                "INTEGRATION_REPLAY_FAILED",
                actorName,
                "integration-replay",
                "IntegrationReplayRecord",
                String.valueOf(record.getId()),
                "Replay failed for inbound order " + record.getExternalOrderId() + ". Reason: "
                    + exception.getReason()
            );
            operationalMetricsService.recordReplayAttempt(tenantCode, false);
            operationalStateChangePublisher.publish(OperationalUpdateType.INTEGRATION_STATE, "integration-replay");
            throw exception;
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
            record.getFailureMessage(),
            record.getStatus(),
            record.getReplayAttemptCount(),
            record.getLastReplayMessage(),
            record.getLastAttemptedAt(),
            record.getResolvedAt(),
            record.getReplayedOrderExternalId(),
            record.getCreatedAt(),
            record.getUpdatedAt()
        );
    }

    private String limit(String value) {
        if (value == null || value.length() <= 320) {
            return value;
        }
        return value.substring(0, 317) + "...";
    }
}
