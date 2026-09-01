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
import com.synapsecore.observability.OperationalAlertHookService;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.tenant.TenantContextService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationReplayService {

    private static final int DEFAULT_QUEUE_LIMIT = 12;
    private static final String AUTO_REPLAY_ACTOR = "system-replay";
    private static final EnumSet<IntegrationFailureCode> MANUAL_ONLY_AUTOMATION_FAILURE_CODES =
        EnumSet.of(IntegrationFailureCode.CONNECTOR_DISABLED);

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
    private final OperationalAlertHookService operationalAlertHookService;
    private final TransactionTemplate transactionTemplate;

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
        var activeReplay = integrationReplayRecordRepository
            .findActiveByTenantCodeIgnoreCaseAndExternalOrderIdForUpdate(
                tenantCode,
                request.externalOrderId(),
                List.of(IntegrationReplayStatus.PENDING, IntegrationReplayStatus.REPLAY_FAILED,
                    IntegrationReplayStatus.DEAD_LETTERED))
            .orElse(null);
        if (activeReplay != null) {
            if (activeReplay.getStatus() == IntegrationReplayStatus.DEAD_LETTERED) {
                integrationInboundRecordService.markRejected(
                    inboundRecordId,
                    activeReplay.getFailureCode(),
                    "Equivalent replay identity is already dead-lettered; use the retained replay record after the prerequisite is repaired, or correct the source before re-ingestion.");
            } else {
                integrationInboundRecordService.markReplayQueued(
                    inboundRecordId,
                    activeReplay.getId(),
                    activeReplay.getFailureCode(),
                    activeReplay.getFailureMessage());
            }
            return toResponse(activeReplay);
        }

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

        businessEventService.recordForTenant(
            tenantCode,
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
        return getReplayQueue(null);
    }

    @Transactional(readOnly = true)
    public List<IntegrationReplayRecordResponse> getReplayQueue(String externalOrderId) {
        var currentOperator = accessDirectoryService.getCurrentOperator();
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        String normalizedExternalOrderId = externalOrderId == null || externalOrderId.isBlank()
            ? null
            : externalOrderId.trim();
        List<IntegrationReplayRecordResponse> replayQueue = normalizedExternalOrderId == null
            ? integrationReplayRecordRepository.findQueueSummariesByTenantCodeIgnoreCaseAndStatusIn(
                tenantCode,
                List.of(IntegrationReplayStatus.PENDING, IntegrationReplayStatus.REPLAY_FAILED, IntegrationReplayStatus.DEAD_LETTERED),
                PageRequest.of(0, DEFAULT_QUEUE_LIMIT))
            : integrationReplayRecordRepository.findQueueSummariesByTenantCodeIgnoreCaseAndExternalOrderIdIgnoreCaseAndStatusIn(
                tenantCode,
                normalizedExternalOrderId,
                List.of(IntegrationReplayStatus.PENDING, IntegrationReplayStatus.REPLAY_FAILED, IntegrationReplayStatus.DEAD_LETTERED),
                PageRequest.of(0, 1));
        return replayQueue
            .stream()
            .filter(record -> currentOperator.isEmpty()
                || accessDirectoryService.hasWarehouseAccess(currentOperator.get(), record.warehouseCode()))
            .toList();
    }

    public IntegrationReplayResultResponse replay(Long replayRecordId, String actorName) {
        AtomicReference<ReplayRecordState> initialState = new AtomicReference<>();
        AtomicReference<IntegrationFailureCodes.IntegrationFailureExceptionDetails> failureDetails =
            new AtomicReference<>();
        AtomicReference<ResponseStatusException> failureException = new AtomicReference<>();
        IntegrationReplayResultResponse replayed;

        try {
            replayed = transactionTemplate.execute(status -> replayManualInTransaction(
                replayRecordId,
                actorName,
                status,
                initialState,
                failureDetails,
                failureException
            ));
        } catch (UnexpectedRollbackException exception) {
            if (failureDetails.get() == null) {
                throw exception;
            }
            replayed = null;
        }

        if (failureDetails.get() != null) {
            if (initialState.get() != null) {
                recordReplayFailureInFreshTransaction(
                    replayRecordId,
                    Instant.now(),
                    initialState.get(),
                    failureDetails.get(),
                    actorName,
                    false
                );
            }
            throw failureException.get();
        }
        return replayed;
    }

    private IntegrationReplayResultResponse replayManualInTransaction(
        Long replayRecordId,
        String actorName,
        TransactionStatus transactionStatus,
        AtomicReference<ReplayRecordState> initialState,
        AtomicReference<IntegrationFailureCodes.IntegrationFailureExceptionDetails> failureDetails,
        AtomicReference<ResponseStatusException> failureException
    ) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        IntegrationReplayRecord record = integrationReplayRecordRepository.findByTenantCodeIgnoreCaseAndIdForUpdate(
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
        initialState.set(ReplayRecordState.from(record));

        if (record.getStatus() == IntegrationReplayStatus.REPLAYED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Integration replay record " + replayRecordId + " has already been resolved.");
        }
        if (record.getStatus() == IntegrationReplayStatus.DEAD_LETTERED) {
            if (record.getFailureCode() == null || !IntegrationFailureCodes.isReplayable(record.getFailureCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Integration replay record " + replayRecordId + " is dead-lettered for a non-replayable failure and requires corrected source data.");
            }
            record.setStatus(IntegrationReplayStatus.PENDING);
            record.setNextEligibleAt(Instant.now());
            record.setLastReplayMessage(limit(
                "Dead-lettered replay explicitly requeued after prerequisite repair; prior attempts retained: "
                    + record.getReplayAttemptCount() + "."));
            integrationReplayRecordRepository.save(record);
            businessEventService.recordForTenant(
                tenantCode,
                BusinessEventType.INTEGRATION_REPLAY_QUEUED,
                "integration-replay",
                "Requeued dead-lettered order " + record.getExternalOrderId() + " for controlled recovery by " + actorName + "."
            );
            auditLogService.recordSuccess(
                "INTEGRATION_REPLAY_REQUEUED",
                actorName,
                "integration-replay",
                "IntegrationReplayRecord",
                String.valueOf(record.getId()),
                "Requeued a dead-lettered replay after prerequisite repair; previous attempts retained: "
                    + record.getReplayAttemptCount() + "."
            );
            operationalStateChangePublisher.publish(OperationalUpdateType.INTEGRATION_STATE, "integration-replay");
        }

        OrderCreateRequest request = deserializeRequest(record);
        Instant attemptedAt = Instant.now();
        if (record.getNextEligibleAt() != null && record.getNextEligibleAt().isAfter(attemptedAt)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Integration replay record " + replayRecordId + " is not eligible for replay until " + record.getNextEligibleAt() + ".");
        }

        try {
            return replayRecord(record, request, attemptedAt, actorName, true, false);
        } catch (ResponseStatusException exception) {
            failureException.set(exception);
            failureDetails.set(IntegrationFailureCodes.extract(exception));
            transactionStatus.setRollbackOnly();
            return null;
        }
    }

    public int processAutomatedReplayBatch(int batchSize) {
        Instant now = Instant.now();
        return integrationReplayRecordRepository.findEligibleIdsForAutomatedReplay(
                List.of(IntegrationReplayStatus.PENDING, IntegrationReplayStatus.REPLAY_FAILED),
                now,
                MANUAL_ONLY_AUTOMATION_FAILURE_CODES,
                PageRequest.of(0, Math.max(batchSize, 1)))
            .stream()
            .mapToInt(replayRecordId -> attemptAutomatedReplayById(replayRecordId, now))
            .sum();
    }

    private int attemptAutomatedReplayById(Long replayRecordId, Instant attemptedAt) {
        AtomicReference<ReplayRecordState> initialState = new AtomicReference<>();
        AtomicReference<IntegrationFailureCodes.IntegrationFailureExceptionDetails> failureDetails =
            new AtomicReference<>();
        AtomicReference<Boolean> contentionFailure = new AtomicReference<>(false);
        Integer replayed;

        try {
            replayed = transactionTemplate.execute(status -> {
                try {
                    int result = attemptAutomatedReplayInTransaction(
                        replayRecordId,
                        attemptedAt,
                        initialState,
                        failureDetails
                    );
                    if (failureDetails.get() != null) {
                        status.setRollbackOnly();
                    }
                    return result;
                } catch (ResponseStatusException exception) {
                    failureDetails.set(IntegrationFailureCodes.extract(exception));
                    status.setRollbackOnly();
                    return 0;
                } catch (PessimisticLockingFailureException exception) {
                    failureDetails.set(contentionFailureDetails());
                    contentionFailure.set(true);
                    status.setRollbackOnly();
                    return 0;
                }
            });
        } catch (UnexpectedRollbackException exception) {
            if (failureDetails.get() == null) {
                throw exception;
            }
            replayed = 0;
        }

        if (failureDetails.get() != null) {
            recordReplayFailureInFreshTransaction(
                replayRecordId,
                attemptedAt,
                initialState.get(),
                failureDetails.get(),
                AUTO_REPLAY_ACTOR,
                contentionFailure.get()
            );
            return 0;
        }
        return replayed == null ? 0 : replayed;
    }

    private int attemptAutomatedReplayInTransaction(
        Long replayRecordId,
        Instant attemptedAt,
        AtomicReference<ReplayRecordState> initialState,
        AtomicReference<IntegrationFailureCodes.IntegrationFailureExceptionDetails> failureDetails
    ) {
        IntegrationReplayRecord record = integrationReplayRecordRepository.findByIdForUpdate(replayRecordId)
            .orElse(null);
        if (record == null) {
            return 0;
        }
        initialState.set(ReplayRecordState.from(record));
        if (!isEligibleForAutomatedReplay(record, attemptedAt)) {
            return 0;
        }
        if (record.getTenantCode() == null || record.getTenantCode().isBlank()) {
            failureDetails.set(new IntegrationFailureCodes.IntegrationFailureExceptionDetails(
                IntegrationFailureCode.UNKNOWN,
                "Replay record is missing tenant context and cannot be processed automatically."
            ));
            return 0;
        }
        if (isManualOnlyAutomatedReplayRecord(record)) {
            return 0;
        }
        OrderCreateRequest request = deserializeRequest(record);
        IntegrationFailureCodes.IntegrationFailureExceptionDetails validationFailure =
            validateAutomatedReplayRequest(request);
        if (validationFailure != null) {
            failureDetails.set(validationFailure);
            return 0;
        }
        String previousRequestId = requestTraceContext.getCurrentRequestId().orElse(null);
        String previousActor = requestTraceContext.getCurrentActor().orElse(null);
        String previousTenant = requestTraceContext.getCurrentTenant().orElse(null);
        try {
            requestTraceContext.setCurrentRequestId("auto-replay-" + record.getId());
            requestTraceContext.setCurrentActor(AUTO_REPLAY_ACTOR);
            requestTraceContext.setCurrentTenant(record.getTenantCode());
            replayRecord(record, request, attemptedAt, AUTO_REPLAY_ACTOR, false, false);
            return 1;
        } finally {
            requestTraceContext.clear();
            restoreTraceValue(previousRequestId, requestTraceContext::setCurrentRequestId);
            restoreTraceValue(previousActor, requestTraceContext::setCurrentActor);
            restoreTraceValue(previousTenant, requestTraceContext::setCurrentTenant);
        }
    }

    private void recordReplayFailureInFreshTransaction(
        Long replayRecordId,
        Instant attemptedAt,
        ReplayRecordState expectedState,
        IntegrationFailureCodes.IntegrationFailureExceptionDetails failure,
        String actorName,
        boolean contention
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            IntegrationReplayRecord record = integrationReplayRecordRepository.findByIdForUpdate(replayRecordId)
                .orElse(null);
            if (!isCurrentFailureCandidate(record, expectedState)) {
                return;
            }

            if (record.getTenantCode() == null || record.getTenantCode().isBlank()) {
                orphanReplayRecord(record, attemptedAt);
                return;
            }

            String previousRequestId = requestTraceContext.getCurrentRequestId().orElse(null);
            String previousActor = requestTraceContext.getCurrentActor().orElse(null);
            String previousTenant = requestTraceContext.getCurrentTenant().orElse(null);
            try {
                requestTraceContext.setCurrentRequestId("auto-replay-" + replayRecordId);
                requestTraceContext.setCurrentActor(actorName);
                requestTraceContext.setCurrentTenant(record.getTenantCode());
                persistReplayFailure(record, attemptedAt, failure, actorName, contention);
            } finally {
                requestTraceContext.clear();
                restoreTraceValue(previousRequestId, requestTraceContext::setCurrentRequestId);
                restoreTraceValue(previousActor, requestTraceContext::setCurrentActor);
                restoreTraceValue(previousTenant, requestTraceContext::setCurrentTenant);
            }
        });
    }

    private boolean isCurrentFailureCandidate(IntegrationReplayRecord record, ReplayRecordState expectedState) {
        if (record == null || !List.of(IntegrationReplayStatus.PENDING, IntegrationReplayStatus.REPLAY_FAILED,
                IntegrationReplayStatus.DEAD_LETTERED)
            .contains(record.getStatus())) {
            return false;
        }
        if (expectedState == null) {
            return true;
        }
        return record.getStatus() == expectedState.status()
            && record.getReplayAttemptCount() == expectedState.replayAttemptCount()
            && Objects.equals(record.getNextEligibleAt(), expectedState.nextEligibleAt());
    }

    private ResponseStatusException contentionFailure() {
        return IntegrationFailureCodes.status(
            HttpStatus.CONFLICT,
            IntegrationFailureCode.UNKNOWN,
            "Inventory is currently under conflicting reservation pressure. Retry once the active order write completes."
        );
    }

    private IntegrationFailureCodes.IntegrationFailureExceptionDetails contentionFailureDetails() {
        return IntegrationFailureCodes.extract(contentionFailure());
    }

    private void emitDeadLetterHookAfterCommit(String alertType,
                                               String severity,
                                               String summary,
                                               String detail) {
        Runnable emit = () -> operationalAlertHookService.emit(alertType, severity, summary, detail);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emit.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emit.run();
            }
        });
    }

    private IntegrationFailureCodes.IntegrationFailureExceptionDetails validateAutomatedReplayRequest(
        OrderCreateRequest request
    ) {
        if (request.warehouseCode() == null || request.warehouseCode().isBlank()) {
            return new IntegrationFailureCodes.IntegrationFailureExceptionDetails(
                IntegrationFailureCode.MISSING_WAREHOUSE_CODE,
                "warehouseCode is required for automated replay."
            );
        }
        if (request.items() == null || request.items().isEmpty()) {
            return new IntegrationFailureCodes.IntegrationFailureExceptionDetails(
                IntegrationFailureCode.MISSING_ITEMS,
                "At least one line item is required for automated replay."
            );
        }
        for (var item : request.items()) {
            if (item.productSku() == null || item.productSku().isBlank()) {
                return new IntegrationFailureCodes.IntegrationFailureExceptionDetails(
                    IntegrationFailureCode.MISSING_PRODUCT_SKU,
                    "productSku is required for every automated replay line item."
                );
            }
            if (item.quantity() == null || item.quantity() < 1) {
                return new IntegrationFailureCodes.IntegrationFailureExceptionDetails(
                    IntegrationFailureCode.INVALID_QUANTITY,
                    "quantity must be at least 1 for every automated replay line item."
                );
            }
            if (item.unitPrice() == null || item.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return new IntegrationFailureCodes.IntegrationFailureExceptionDetails(
                    IntegrationFailureCode.INVALID_UNIT_PRICE,
                    "unitPrice must be greater than zero for every automated replay line item."
                );
            }
        }
        return null;
    }

    private void persistReplayFailure(IntegrationReplayRecord record,
                                      Instant attemptedAt,
                                      IntegrationFailureCodes.IntegrationFailureExceptionDetails failure,
                                      String actorName,
                                      boolean contention) {
        String tenantCode = record.getTenantCode();
        int nextAttemptCount = record.getReplayAttemptCount() + 1;
        record.setReplayAttemptCount(nextAttemptCount);
        record.setLastAttemptedAt(attemptedAt);
        record.setFailureCode(failure.failureCode());
        boolean exhausted = nextAttemptCount >= Math.max(maxReplayAttempts, 1);
        if (exhausted) {
            record.setStatus(IntegrationReplayStatus.DEAD_LETTERED);
            record.setDeadLetteredAt(attemptedAt);
            record.setNextEligibleAt(null);
            record.setLastReplayMessage(limit(failure.failureMessage()
                + (contention
                    ? " Dead-lettered after " + nextAttemptCount + " contention retries."
                    : " Dead-lettered after " + nextAttemptCount + " attempts.")));
        } else {
            record.setStatus(IntegrationReplayStatus.REPLAY_FAILED);
            record.setNextEligibleAt(nextEligibleAt(attemptedAt, nextAttemptCount));
            record.setLastReplayMessage(limit(failure.failureMessage()));
        }
        record = integrationReplayRecordRepository.save(record);
        if (exhausted) {
            emitDeadLetterHookAfterCommit(
                "INTEGRATION_REPLAY_DEAD_LETTERED",
                "HIGH",
                "Integration replay " + record.getId() + (contention ? " was dead-lettered after contention retries." : " was dead-lettered."),
                contention
                    ? "Tenant " + tenantCode + " source " + record.getSourceSystem() + " order " + record.getExternalOrderId() + " exhausted contention retries."
                    : "Tenant " + tenantCode + " source " + record.getSourceSystem() + " order " + record.getExternalOrderId() + " failed with " + failure.failureCode() + "."
            );
        }

        businessEventService.recordForTenant(
            tenantCode,
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
        log.warn("Integration replay {} {} for tenant {} source {}: {}",
            record.getId(), contention ? "hit contention" : "failed", tenantCode, record.getSourceSystem(), failure.failureMessage());
    }

    private boolean isEligibleForAutomatedReplay(IntegrationReplayRecord record, Instant attemptedAt) {
        if (record == null) {
            return false;
        }
        if (!List.of(IntegrationReplayStatus.PENDING, IntegrationReplayStatus.REPLAY_FAILED).contains(record.getStatus())) {
            return false;
        }
        if (record.getNextEligibleAt() == null || record.getNextEligibleAt().isAfter(attemptedAt)) {
            return false;
        }
        if (isManualOnlyAutomatedReplayRecord(record)) {
            return false;
        }
        try {
            integrationConnectorService.requireEnabledConnectorForTenant(
                record.getTenantCode(),
                record.getSourceSystem(),
                record.getConnectorType(),
                "automatically replay failed inbound orders"
            );
            return true;
        } catch (ResponseStatusException exception) {
            return false;
        }
    }

    private IntegrationReplayResultResponse replayRecord(IntegrationReplayRecord record,
                                                         OrderCreateRequest request,
                                                         Instant attemptedAt,
                                                         String actorName,
                                                         boolean enforceWarehouseAccess,
                                                         boolean persistFailure) {
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
            OrderResponse existingOrder = orderService
                .findOrderForTenantByExternalOrderId(tenantCode, request.externalOrderId())
                .orElse(null);
            boolean alreadyCompleted = existingOrder != null;
            OrderResponse order = existingOrder;
            if (order == null) {
                integrationConnectorService.requireEnabledConnectorForTenant(
                    tenantCode,
                    record.getSourceSystem(),
                    record.getConnectorType(),
                    "replay failed inbound orders");
                order = orderService.createOrderForTenant(
                    tenantCode,
                    request,
                    (enforceWarehouseAccess ? "integration-replay:" : "integration-replay:auto:") + record.getSourceSystem()
                );
            }
            record.setStatus(IntegrationReplayStatus.REPLAYED);
            record.setReplayAttemptCount(record.getReplayAttemptCount() + 1);
            record.setLastAttemptedAt(attemptedAt);
            record.setNextEligibleAt(null);
            record.setResolvedAt(attemptedAt);
            record.setReplayedOrderExternalId(order.externalOrderId());
            record.setFailureCode(null);
            record.setLastReplayMessage(limit(alreadyCompleted
                ? "Business order " + order.externalOrderId() + " already existed; replay reconciled without creating a duplicate."
                : "Replayed successfully as live order " + order.externalOrderId() + "."));
            record = integrationReplayRecordRepository.save(record);
            integrationInboundRecordService.markReplayed(record.getInboundRecordId(), order.externalOrderId());

            businessEventService.recordForTenant(
                tenantCode,
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
            log.info("Integration replay {} completed for tenant {} source {} as order {}.", record.getId(), tenantCode, record.getSourceSystem(), order.externalOrderId());
            return new IntegrationReplayResultResponse(toResponse(record), order, attemptedAt);
        } catch (ResponseStatusException exception) {
            var failure = IntegrationFailureCodes.extract(exception);
            if (persistFailure) {
                persistReplayFailure(record, attemptedAt, failure, actorName, false);
            }
            throw exception;
        } catch (PessimisticLockingFailureException exception) {
            ResponseStatusException contention = contentionFailure();
            if (persistFailure) {
                persistReplayFailure(record, attemptedAt, IntegrationFailureCodes.extract(contention), actorName, true);
            }
            throw contention;
        }
    }

    private void restoreTraceValue(String value, java.util.function.Consumer<String> setter) {
        if (value != null && !value.isBlank()) {
            setter.accept(value);
        }
    }

    private void orphanReplayRecord(IntegrationReplayRecord record, Instant attemptedAt) {
        record.setReplayAttemptCount(record.getReplayAttemptCount() + 1);
        record.setLastAttemptedAt(attemptedAt);
        record.setFailureCode(IntegrationFailureCode.UNKNOWN);
        record.setStatus(IntegrationReplayStatus.DEAD_LETTERED);
        record.setDeadLetteredAt(attemptedAt);
        record.setNextEligibleAt(null);
        record.setLastReplayMessage(limit("Replay record is missing tenant context and cannot be processed automatically."));
        integrationReplayRecordRepository.save(record);
        emitDeadLetterHookAfterCommit(
            "INTEGRATION_REPLAY_ORPHANED",
            "HIGH",
            "Integration replay " + record.getId() + " is missing tenant context.",
            "Source " + record.getSourceSystem() + " order " + record.getExternalOrderId()
                + " cannot be processed automatically because the replay record has no tenant code."
        );
        businessEventService.recordForTenant(
            RequestTraceContext.MISSING_TENANT_CONTEXT,
            BusinessEventType.INTEGRATION_REPLAY_FAILED,
            "integration-replay",
            "Replay record " + record.getId() + " is missing tenant context and was dead-lettered before automated replay."
        );
        log.error("Integration replay {} is missing tenant context and was dead-lettered before automated replay.", record.getId());
    }

    private boolean isManualOnlyAutomatedReplayRecord(IntegrationReplayRecord record) {
        return record.getFailureCode() != null
            && MANUAL_ONLY_AUTOMATION_FAILURE_CODES.contains(record.getFailureCode());
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

    private record ReplayRecordState(IntegrationReplayStatus status,
                                     int replayAttemptCount,
                                     Instant nextEligibleAt) {

        private static ReplayRecordState from(IntegrationReplayRecord record) {
            return new ReplayRecordState(
                record.getStatus(),
                record.getReplayAttemptCount(),
                record.getNextEligibleAt()
            );
        }
    }
}
