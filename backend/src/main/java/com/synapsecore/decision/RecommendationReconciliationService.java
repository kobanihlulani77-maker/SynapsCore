package com.synapsecore.decision;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.entity.RecommendationStatus;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.fulfillment.FulfillmentService;
import com.synapsecore.intelligence.InventoryMonitoringService;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Re-evaluates advisory state from authoritative inventory and fulfillment data.
 * This is intentionally a bounded single-backend reconciliation pass, not a
 * general-purpose job or distributed worker system.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationReconciliationService {

    private static final List<FulfillmentStatus> ACTIVE_STATUSES = List.of(
        FulfillmentStatus.QUEUED,
        FulfillmentStatus.PICKING,
        FulfillmentStatus.PACKED,
        FulfillmentStatus.DISPATCHED,
        FulfillmentStatus.DELAYED,
        FulfillmentStatus.EXCEPTION
    );

    private static final String SCHEDULER_REQUEST_PREFIX = "rec-scheduler-";
    private static final String SCHEDULER_ACTOR = "system-scheduler";

    private final InventoryRepository inventoryRepository;
    private final FulfillmentTaskRepository fulfillmentTaskRepository;
    private final RecommendationRepository recommendationRepository;
    private final InventoryMonitoringService inventoryMonitoringService;
    private final FulfillmentService fulfillmentService;
    private final RecommendationService recommendationService;
    private final RecommendationReconciliationEvidenceService evidenceService;
    private final RequestTraceContext requestTraceContext;

    @Value("${synapsecore.recommendation.reconciliation.enabled:true}")
    private boolean enabled;

    @Scheduled(
        initialDelayString = "${synapsecore.recommendation.reconciliation.initial-delay-ms:60000}",
        fixedDelayString = "${synapsecore.recommendation.reconciliation.interval-ms:60000}"
    )
    public void reconcileOnSchedule() {
        if (!enabled) {
            return;
        }

        String runId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        requestTraceContext.setCurrentRequestId(SCHEDULER_REQUEST_PREFIX + runId);
        requestTraceContext.setCurrentActor(SCHEDULER_ACTOR);

        try {
            ReconciliationInput input = loadInput();
            ReconciliationAccumulator accumulator = new ReconciliationAccumulator(input.tenantCodes());
            safelyRecordStarted(runId, startedAt, input.tenantCodes());
            try {
                reconcile(input, accumulator);
            } catch (RuntimeException exception) {
                accumulator.markFatalFailure();
                log.error("Recommendation reconciliation scheduler run {} failed before completion: {}",
                    runId, exception.getMessage(), exception);
            }
            Instant completedAt = Instant.now();
            safelyRecordCompleted(
                runId,
                startedAt,
                completedAt,
                accumulator.outcome(),
                accumulator.snapshotByTenant()
            );
        } catch (RuntimeException exception) {
            log.error("Recommendation reconciliation scheduler run {} could not establish its evidence boundary: {}",
                runId, exception.getMessage(), exception);
        } finally {
            requestTraceContext.clear();
        }
    }

    /**
     * Direct/internal reconciliation remains separate from scheduled evidence.
     * Tests and callers using this method must not masquerade as a clock-triggered run.
     */
    public int reconcileNow() {
        ReconciliationInput input = loadInput();
        return reconcile(input, new ReconciliationAccumulator(input.tenantCodes())).totalProcessed();
    }

    private ReconciliationInput loadInput() {
        List<Inventory> inventories = inventoryRepository.findAllWithProductAndWarehouse();
        Collection<com.synapsecore.domain.entity.FulfillmentTask> activeTasks =
            fulfillmentTaskRepository.findAllByStatusInOrderByUpdatedAtDesc(ACTIVE_STATUSES);
        List<Recommendation> fulfillmentRecommendations = recommendationRepository.findAllBySourceTypeAndStatus(
            "FULFILLMENT", RecommendationStatus.CURRENT);
        return new ReconciliationInput(inventories, List.copyOf(activeTasks), fulfillmentRecommendations);
    }

    private ReconciliationAccumulator reconcile(ReconciliationInput input,
                                                ReconciliationAccumulator accumulator) {
        for (var inventory : input.inventories()) {
            String tenantCode = inventory.getWarehouse().getTenant().getCode();
            accumulator.inventoryAttempted(tenantCode);
            try {
                inventoryMonitoringService.evaluateAfterChange(inventory, "recommendation-reconciliation");
                accumulator.inventorySucceeded(tenantCode);
            } catch (RuntimeException exception) {
                accumulator.inventoryFailed(tenantCode);
                log.warn("Recommendation reconciliation skipped inventory {} in tenant {}: {}",
                    inventory.getId(), tenantCode, exception.getMessage());
            }
        }

        Set<Long> activeWarehouseIds = new HashSet<>();
        for (var task : input.activeTasks()) {
            String tenantCode = task.getTenant().getCode();
            activeWarehouseIds.add(task.getWarehouse().getId());
            accumulator.fulfillmentAttempted(tenantCode);
            try {
                fulfillmentService.reconcileRecommendation(task.getId(), "recommendation-reconciliation");
                accumulator.fulfillmentSucceeded(tenantCode);
            } catch (RuntimeException exception) {
                accumulator.fulfillmentFailed(tenantCode);
                log.warn("Recommendation reconciliation skipped fulfillment task {} in tenant {}: {}",
                    task.getId(), tenantCode, exception.getMessage());
            }
        }

        for (Recommendation recommendation : input.fulfillmentRecommendations()) {
            if (!activeWarehouseIds.contains(recommendation.getWarehouse().getId())) {
                String tenantCode = recommendation.getWarehouse().getTenant().getCode();
                accumulator.retirementAttempted(tenantCode);
                try {
                    recommendationService.retireCurrentRecommendation(recommendation.getId());
                    accumulator.retirementSucceeded(tenantCode);
                } catch (RuntimeException exception) {
                    accumulator.retirementFailed(tenantCode);
                    log.warn("Recommendation reconciliation could not retire fulfillment recommendation {}: {}",
                        recommendation.getId(), exception.getMessage());
                }
            }
        }

        return accumulator;
    }

    private void safelyRecordStarted(String runId, Instant startedAt, Set<String> tenantCodes) {
        try {
            evidenceService.recordStarted(runId, startedAt, tenantCodes);
        } catch (RuntimeException exception) {
            log.warn("Recommendation scheduler start evidence failed for run {}: {}",
                runId, exception.getMessage());
        }
    }

    private void safelyRecordCompleted(String runId,
                                       Instant startedAt,
                                       Instant completedAt,
                                       String outcome,
                                       Map<String, RecommendationReconciliationEvidenceService.ReconciliationTenantCounts> countsByTenant) {
        try {
            evidenceService.recordCompleted(runId, startedAt, completedAt, outcome, countsByTenant);
        } catch (RuntimeException exception) {
            log.warn("Recommendation scheduler completion evidence failed for run {}: {}",
                runId, exception.getMessage());
        }
    }

    private record ReconciliationInput(
        List<Inventory> inventories,
        List<com.synapsecore.domain.entity.FulfillmentTask> activeTasks,
        List<Recommendation> fulfillmentRecommendations
    ) {

        private Set<String> tenantCodes() {
            Set<String> tenantCodes = new LinkedHashSet<>();
            inventories.forEach(inventory -> tenantCodes.add(inventory.getWarehouse().getTenant().getCode()));
            activeTasks.forEach(task -> tenantCodes.add(task.getTenant().getCode()));
            fulfillmentRecommendations.forEach(recommendation ->
                tenantCodes.add(recommendation.getWarehouse().getTenant().getCode())
            );
            return tenantCodes;
        }
    }

    private static final class ReconciliationAccumulator {

        private final Map<String, MutableTenantCounts> countsByTenant = new HashMap<>();
        private boolean fatalFailure;
        private int totalProcessed;

        private ReconciliationAccumulator(Set<String> tenantCodes) {
            tenantCodes.forEach(this::countsFor);
        }

        private MutableTenantCounts countsFor(String tenantCode) {
            return countsByTenant.computeIfAbsent(tenantCode, ignored -> new MutableTenantCounts());
        }

        private void inventoryAttempted(String tenantCode) {
            countsFor(tenantCode).inventoryAttempted++;
        }

        private void inventorySucceeded(String tenantCode) {
            countsFor(tenantCode).inventorySucceeded++;
            totalProcessed++;
        }

        private void inventoryFailed(String tenantCode) {
            countsFor(tenantCode).inventoryFailed++;
        }

        private void fulfillmentAttempted(String tenantCode) {
            countsFor(tenantCode).fulfillmentAttempted++;
        }

        private void fulfillmentSucceeded(String tenantCode) {
            countsFor(tenantCode).fulfillmentSucceeded++;
            totalProcessed++;
        }

        private void fulfillmentFailed(String tenantCode) {
            countsFor(tenantCode).fulfillmentFailed++;
        }

        private void retirementAttempted(String tenantCode) {
            countsFor(tenantCode).retirementsAttempted++;
        }

        private void retirementSucceeded(String tenantCode) {
            countsFor(tenantCode).retirementsSucceeded++;
            totalProcessed++;
        }

        private void retirementFailed(String tenantCode) {
            countsFor(tenantCode).retirementsFailed++;
        }

        private void markFatalFailure() {
            fatalFailure = true;
        }

        private int totalProcessed() {
            return totalProcessed;
        }

        private String outcome() {
            if (fatalFailure) {
                return RecommendationReconciliationEvidenceService.FAILED;
            }
            return countsByTenant.values().stream().anyMatch(MutableTenantCounts::hasFailures)
                ? RecommendationReconciliationEvidenceService.COMPLETED_WITH_FAILURES
                : RecommendationReconciliationEvidenceService.COMPLETED;
        }

        private Map<String, RecommendationReconciliationEvidenceService.ReconciliationTenantCounts> snapshotByTenant() {
            Map<String, RecommendationReconciliationEvidenceService.ReconciliationTenantCounts> snapshot = new HashMap<>();
            countsByTenant.forEach((tenantCode, counts) -> snapshot.put(tenantCode, counts.snapshot()));
            return snapshot;
        }
    }

    private static final class MutableTenantCounts {

        private int inventoryAttempted;
        private int inventorySucceeded;
        private int inventoryFailed;
        private int fulfillmentAttempted;
        private int fulfillmentSucceeded;
        private int fulfillmentFailed;
        private int retirementsAttempted;
        private int retirementsSucceeded;
        private int retirementsFailed;

        private boolean hasFailures() {
            return inventoryFailed > 0 || fulfillmentFailed > 0 || retirementsFailed > 0;
        }

        private RecommendationReconciliationEvidenceService.ReconciliationTenantCounts snapshot() {
            return new RecommendationReconciliationEvidenceService.ReconciliationTenantCounts(
                inventoryAttempted,
                inventorySucceeded,
                inventoryFailed,
                fulfillmentAttempted,
                fulfillmentSucceeded,
                fulfillmentFailed,
                retirementsAttempted,
                retirementsSucceeded,
                retirementsFailed
            );
        }
    }
}
