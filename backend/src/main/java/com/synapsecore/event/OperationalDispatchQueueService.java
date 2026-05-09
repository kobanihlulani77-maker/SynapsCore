package com.synapsecore.event;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.entity.OperationalDispatchWorkItem;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.domain.service.DashboardService;
import com.synapsecore.domain.service.CoreIdentityWriteIsolationService;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.realtime.RealtimeService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
    private final CoreIdentityWriteIsolationService coreIdentityWriteIsolationService;

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
        coreIdentityWriteIsolationService.persistWithSequenceRepair(
            "Operational dispatch enqueue",
            () -> operationalDispatchWorkItemRepository.save(workItem)
        );
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
            List<DispatchBatch> dispatchBatches = collapseIntoDispatchBatches(pendingItems);
            for (DispatchBatch dispatchBatch : dispatchBatches) {
                processedCount += processDispatchBatch(dispatchBatch);
            }
            if (!pendingItems.isEmpty()) {
                log.debug("Operational dispatch queue collapsed {} pending item(s) into {} broadcast batch(es).",
                    pendingItems.size(), dispatchBatches.size());
            }
        } finally {
            draining.set(false);
        }
        return processedCount;
    }

    public boolean isDraining() {
        return draining.get();
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

    private int processDispatchBatch(DispatchBatch dispatchBatch) {
        List<OperationalDispatchWorkItem> claimedItems = claimDispatchBatch(dispatchBatch.workItems());
        if (claimedItems.isEmpty()) {
            return 0;
        }

        OperationalDispatchWorkItem representativeItem = claimedItems.get(0);
        requestTraceContext.setCurrentRequestId(representativeItem.getRequestId());
        requestTraceContext.setCurrentActor(SYSTEM_ACTOR);
        requestTraceContext.setCurrentTenant(representativeItem.getTenantCode());
        MDC.put(REQUEST_ID_MDC_KEY, representativeItem.getRequestId());
        MDC.put(ACTOR_MDC_KEY, SYSTEM_ACTOR);
        MDC.put(TENANT_MDC_KEY, representativeItem.getTenantCode());

        try {
            if (dispatchBatch.surface() == DispatchSurface.INTEGRATION) {
                realtimeServiceProvider.getObject().broadcastIntegrationUpdates(dispatchBatch.tenantCode());
            } else {
                dashboardServiceProvider.getObject().refreshSummary();
                realtimeServiceProvider.getObject().broadcastOperationalUpdates(dispatchBatch.tenantCode());
            }
            markDispatchBatchCompleted(claimedItems);
            log.debug("Operational dispatch queue processed {} {} item(s) for tenant {} using request {}",
                claimedItems.size(), dispatchBatch.surface(), dispatchBatch.tenantCode(), representativeItem.getRequestId());
            return claimedItems.size();
        } catch (RuntimeException exception) {
            markDispatchBatchFailed(claimedItems, exception);
            log.warn("Operational dispatch queue failed {} {} item(s) for tenant {} request {}: {}",
                claimedItems.size(), dispatchBatch.surface(), dispatchBatch.tenantCode(),
                representativeItem.getRequestId(), exception.getMessage());
            return 0;
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(ACTOR_MDC_KEY);
            MDC.remove(TENANT_MDC_KEY);
            requestTraceContext.clear();
        }
    }

    private List<DispatchBatch> collapseIntoDispatchBatches(List<OperationalDispatchWorkItem> pendingItems) {
        Map<String, List<OperationalDispatchWorkItem>> groupedItems = new LinkedHashMap<>();
        for (OperationalDispatchWorkItem pendingItem : pendingItems) {
            groupedItems.computeIfAbsent(pendingItem.getTenantCode(), ignored -> new ArrayList<>()).add(pendingItem);
        }
        return groupedItems.entrySet().stream()
            .map(entry -> new DispatchBatch(entry.getKey(), determineDispatchSurface(entry.getValue()), entry.getValue()))
            .toList();
    }

    private List<OperationalDispatchWorkItem> claimDispatchBatch(List<OperationalDispatchWorkItem> workItems) {
        List<OperationalDispatchWorkItem> claimedItems = new ArrayList<>();
        for (OperationalDispatchWorkItem workItem : workItems) {
            workItem.setStatus(OperationalDispatchStatus.PROCESSING);
            workItem.setAttemptCount(workItem.getAttemptCount() + 1);
            try {
                operationalDispatchWorkItemRepository.save(workItem);
                claimedItems.add(workItem);
            } catch (ObjectOptimisticLockingFailureException lockingFailureException) {
                log.debug("Operational dispatch work item {} was already claimed by another worker before processing started.",
                    workItem.getId());
            }
        }
        return claimedItems;
    }

    private void markDispatchBatchCompleted(List<OperationalDispatchWorkItem> workItems) {
        Instant processedAt = Instant.now();
        for (OperationalDispatchWorkItem workItem : workItems) {
            workItem.setStatus(OperationalDispatchStatus.COMPLETED);
            workItem.setProcessedAt(processedAt);
            workItem.setLastError(null);
            try {
                operationalDispatchWorkItemRepository.save(workItem);
                operationalMetricsService.recordDispatchProcessed(workItem.getTenantCode(), workItem.getUpdateType());
            } catch (ObjectOptimisticLockingFailureException lockingFailureException) {
                log.debug("Operational dispatch work item {} was already completed by another worker.",
                    workItem.getId());
            }
        }
    }

    private void markDispatchBatchFailed(List<OperationalDispatchWorkItem> workItems, RuntimeException exception) {
        String errorMessage = limit(exception.getMessage());
        for (OperationalDispatchWorkItem workItem : workItems) {
            workItem.setStatus(OperationalDispatchStatus.FAILED);
            workItem.setLastError(errorMessage);
            try {
                operationalDispatchWorkItemRepository.save(workItem);
                operationalMetricsService.recordDispatchFailure(workItem.getTenantCode(), workItem.getUpdateType());
            } catch (ObjectOptimisticLockingFailureException lockingFailureException) {
                log.debug("Operational dispatch work item {} was already updated by another worker while recording failure.",
                    workItem.getId());
            }
        }
    }

    private DispatchSurface determineDispatchSurface(List<OperationalDispatchWorkItem> workItems) {
        return workItems.stream().allMatch(workItem -> workItem.getUpdateType() == OperationalUpdateType.INTEGRATION_STATE)
            ? DispatchSurface.INTEGRATION
            : DispatchSurface.OPERATIONAL;
    }

    private String limit(String value) {
        if (value == null || value.length() <= 320) {
            return value;
        }
        return value.substring(0, 317) + "...";
    }

    private enum DispatchSurface {
        OPERATIONAL,
        INTEGRATION
    }

    private record DispatchBatch(String tenantCode,
                                 DispatchSurface surface,
                                 List<OperationalDispatchWorkItem> workItems) {
    }
}
