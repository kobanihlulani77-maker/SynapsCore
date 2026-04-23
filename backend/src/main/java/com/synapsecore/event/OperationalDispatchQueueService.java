package com.synapsecore.event;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.entity.OperationalDispatchWorkItem;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.domain.service.DashboardService;
import com.synapsecore.domain.service.IdentitySequenceMigrationService;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.realtime.RealtimeService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationalDispatchQueueService {

    private static final String SYSTEM_ACTOR = "system-queue";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String ACTOR_MDC_KEY = "actor";
    private static final String TENANT_MDC_KEY = "tenant";

    private final OperationalDispatchWorkItemRepository operationalDispatchWorkItemRepository;
    private final ObjectProvider<DashboardService> dashboardServiceProvider;
    private final ObjectProvider<RealtimeService> realtimeServiceProvider;
    private final RequestTraceContext requestTraceContext;
    private final OperationalMetricsService operationalMetricsService;
    private final IdentitySequenceMigrationService identitySequenceMigrationService;

    private final AtomicBoolean draining = new AtomicBoolean(false);

    @Value("${synapsecore.queue.batch-size:16}")
    private int batchSize;

    public void enqueue(OperationalStateChangedEvent event) {
        OperationalDispatchWorkItem workItem = OperationalDispatchWorkItem.builder()
            .tenantCode(event.tenantCode())
            .updateType(event.updateType())
            .source(event.source())
            .requestId(event.requestId())
            .status(OperationalDispatchStatus.PENDING)
            .occurredAt(event.occurredAt())
            .build();
        try {
            operationalDispatchWorkItemRepository.save(workItem);
        } catch (DataIntegrityViolationException exception) {
            log.warn("Operational dispatch enqueue conflicted; synchronizing core identity sequences and retrying once.");
            identitySequenceMigrationService.synchronizeCoreIdentitySequences();
            operationalDispatchWorkItemRepository.save(workItem);
        }
        operationalMetricsService.recordDispatchQueued(event.tenantCode(), event.updateType());
    }

    @Scheduled(fixedDelayString = "${synapsecore.queue.dispatch-interval-ms:1500}")
    public void drainOnSchedule() {
        processPendingWork();
    }

    public int processPendingWork() {
        if (!draining.compareAndSet(false, true)) {
            return 0;
        }

        int processedCount = 0;
        try {
            List<OperationalDispatchWorkItem> pendingItems = operationalDispatchWorkItemRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(OperationalDispatchStatus.PENDING),
                PageRequest.of(0, Math.max(batchSize, 1))
            );
            for (OperationalDispatchWorkItem workItem : pendingItems) {
                processedCount += processSingleWorkItem(workItem);
            }
        } finally {
            draining.set(false);
        }
        return processedCount;
    }

    public Long oldestPendingAgeSeconds(String tenantCode) {
        return operationalDispatchWorkItemRepository
            .findTopByTenantCodeIgnoreCaseAndStatusInOrderByCreatedAtAsc(
                tenantCode,
                List.of(OperationalDispatchStatus.PENDING, OperationalDispatchStatus.PROCESSING)
            )
            .map(OperationalDispatchWorkItem::getCreatedAt)
            .map(createdAt -> createdAt.until(Instant.now(), ChronoUnit.SECONDS))
            .orElse(null);
    }

    private int processSingleWorkItem(OperationalDispatchWorkItem workItem) {
        workItem.setStatus(OperationalDispatchStatus.PROCESSING);
        workItem.setAttemptCount(workItem.getAttemptCount() + 1);
        operationalDispatchWorkItemRepository.save(workItem);

        requestTraceContext.setCurrentRequestId(workItem.getRequestId());
        requestTraceContext.setCurrentActor(SYSTEM_ACTOR);
        requestTraceContext.setCurrentTenant(workItem.getTenantCode());
        MDC.put(REQUEST_ID_MDC_KEY, workItem.getRequestId());
        MDC.put(ACTOR_MDC_KEY, SYSTEM_ACTOR);
        MDC.put(TENANT_MDC_KEY, workItem.getTenantCode());

        try {
            if (workItem.getUpdateType() == OperationalUpdateType.INTEGRATION_STATE) {
                realtimeServiceProvider.getObject().broadcastIntegrationUpdates(workItem.getTenantCode());
            } else {
                dashboardServiceProvider.getObject().refreshSummary();
                realtimeServiceProvider.getObject().broadcastOperationalUpdates(workItem.getTenantCode());
            }
            workItem.setStatus(OperationalDispatchStatus.COMPLETED);
            workItem.setProcessedAt(Instant.now());
            workItem.setLastError(null);
            operationalDispatchWorkItemRepository.save(workItem);
            operationalMetricsService.recordDispatchProcessed(workItem.getTenantCode(), workItem.getUpdateType());
            log.debug("Operational dispatch queue processed {} for tenant {} from {} request {}",
                workItem.getUpdateType(), workItem.getTenantCode(), workItem.getSource(), workItem.getRequestId());
            return 1;
        } catch (RuntimeException exception) {
            workItem.setStatus(OperationalDispatchStatus.FAILED);
            workItem.setLastError(limit(exception.getMessage()));
            operationalDispatchWorkItemRepository.save(workItem);
            operationalMetricsService.recordDispatchFailure(workItem.getTenantCode(), workItem.getUpdateType());
            log.warn("Operational dispatch queue failed {} for tenant {} request {}: {}",
                workItem.getUpdateType(), workItem.getTenantCode(), workItem.getRequestId(), exception.getMessage());
            return 0;
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(ACTOR_MDC_KEY);
            MDC.remove(TENANT_MDC_KEY);
            requestTraceContext.clear();
        }
    }

    private String limit(String value) {
        if (value == null || value.length() <= 320) {
            return value;
        }
        return value.substring(0, 317) + "...";
    }
}
