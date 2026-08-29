package com.synapsecore.decision;

import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.entity.RecommendationStatus;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.fulfillment.FulfillmentService;
import com.synapsecore.intelligence.InventoryMonitoringService;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private final InventoryRepository inventoryRepository;
    private final FulfillmentTaskRepository fulfillmentTaskRepository;
    private final RecommendationRepository recommendationRepository;
    private final InventoryMonitoringService inventoryMonitoringService;
    private final FulfillmentService fulfillmentService;
    private final RecommendationService recommendationService;

    @Value("${synapsecore.recommendation.reconciliation.enabled:true}")
    private boolean enabled;

    @Scheduled(
        initialDelayString = "${synapsecore.recommendation.reconciliation.initial-delay-ms:60000}",
        fixedDelayString = "${synapsecore.recommendation.reconciliation.interval-ms:60000}"
    )
    public void reconcileOnSchedule() {
        if (enabled) {
            reconcileNow();
        }
    }

    public int reconcileNow() {
        int processed = 0;
        for (var inventory : inventoryRepository.findAllWithProductAndWarehouse()) {
            try {
                inventoryMonitoringService.evaluateAfterChange(inventory, "recommendation-reconciliation");
                processed++;
            } catch (RuntimeException exception) {
                log.warn("Recommendation reconciliation skipped inventory {} in tenant {}: {}",
                    inventory.getId(),
                    inventory.getWarehouse().getTenant().getCode(),
                    exception.getMessage());
            }
        }

        Set<Long> activeWarehouseIds = new HashSet<>();
        Collection<com.synapsecore.domain.entity.FulfillmentTask> activeTasks =
            fulfillmentTaskRepository.findAllByStatusInOrderByUpdatedAtDesc(ACTIVE_STATUSES);
        for (var task : activeTasks) {
            activeWarehouseIds.add(task.getWarehouse().getId());
            try {
                fulfillmentService.reconcileRecommendation(task.getId(), "recommendation-reconciliation");
                processed++;
            } catch (RuntimeException exception) {
                log.warn("Recommendation reconciliation skipped fulfillment task {} in tenant {}: {}",
                    task.getId(),
                    task.getTenant().getCode(),
                    exception.getMessage());
            }
        }

        for (Recommendation recommendation : recommendationRepository.findAllBySourceTypeAndStatus(
            "FULFILLMENT", RecommendationStatus.CURRENT)) {
            if (!activeWarehouseIds.contains(recommendation.getWarehouse().getId())) {
                try {
                    recommendationService.retireCurrentRecommendation(recommendation.getId());
                    processed++;
                } catch (RuntimeException exception) {
                    log.warn("Recommendation reconciliation could not retire fulfillment recommendation {}: {}",
                        recommendation.getId(), exception.getMessage());
                }
            }
        }

        return processed;
    }
}
