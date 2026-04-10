package com.synapsecore.decision;

import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.entity.RecommendationPriority;
import com.synapsecore.domain.entity.RecommendationType;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.fulfillment.FulfillmentAssessment;
import com.synapsecore.intelligence.InventoryInsight;
import com.synapsecore.prediction.StockPrediction;
import com.synapsecore.scenario.dto.ScenarioRecommendationProjection;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final InventoryRepository inventoryRepository;
    private final RecommendationRepository recommendationRepository;
    private final BusinessEventService businessEventService;

    public Recommendation createForInventory(Inventory inventory, InventoryInsight insight, StockPrediction prediction, String source) {
        ScenarioRecommendationProjection projection = previewForInventory(inventory, insight, prediction);
        if (projection == null) {
            return null;
        }

        Recommendation latest = recommendationRepository.findFirstByTenant_CodeIgnoreCaseAndTypeAndTitleOrderByCreatedAtDesc(
                inventory.getWarehouse().getTenant() == null ? null : inventory.getWarehouse().getTenant().getCode(),
                projection.type(),
                projection.title())
            .orElse(null);
        if (latest != null
            && latest.getDescription().equals(projection.description())
            && latest.getCreatedAt().isAfter(Instant.now().minusSeconds(1800))) {
            return latest;
        }

        Recommendation recommendation = recommendationRepository.save(Recommendation.builder()
            .tenant(inventory.getWarehouse().getTenant())
            .type(projection.type())
            .title(projection.title())
            .description(projection.description())
            .priority(projection.priority())
            .build());

        businessEventService.record(
            BusinessEventType.RECOMMENDATION_GENERATED,
            source,
            "Generated " + projection.priority() + " recommendation for " + inventory.getProduct().getSku()
                + " in " + inventory.getWarehouse().getCode()
        );

        return recommendation;
    }

    public ScenarioRecommendationProjection previewForInventory(Inventory inventory,
                                                                InventoryInsight insight,
                                                                StockPrediction prediction) {
        if (!insight.lowStock() && !insight.depletionRisk()) {
            return null;
        }

        Optional<TransferPlan> transferPlan = insight.lowStock()
            ? findTransferPlan(inventory)
            : Optional.empty();

        RecommendationPriority priority;
        if (insight.lowStock()) {
            priority = insight.elevatedUrgency()
                ? RecommendationPriority.CRITICAL
                : prediction.depletionRisk() ? RecommendationPriority.HIGH : RecommendationPriority.MEDIUM;
        } else {
            priority = prediction.urgentRisk() ? RecommendationPriority.HIGH : RecommendationPriority.MEDIUM;
        }

        RecommendationType type = transferPlan.isPresent()
            ? RecommendationType.TRANSFER_STOCK
            : insight.lowStock() && priority == RecommendationPriority.CRITICAL
                ? RecommendationType.REORDER_URGENTLY
                : RecommendationType.REORDER_STOCK;

        return new ScenarioRecommendationProjection(
            type,
            priority,
            buildTitle(inventory, insight, priority, transferPlan),
            buildDescription(inventory, insight, priority, prediction, transferPlan)
        );
    }

    public Recommendation createForFulfillment(FulfillmentTask task, FulfillmentAssessment assessment, String source) {
        if (!assessment.backlogRisk() && !assessment.deliveryDelayRisk() && !assessment.anomalyDetected()) {
            return null;
        }

        RecommendationType type = assessment.anomalyDetected()
            ? RecommendationType.INVESTIGATE_LOGISTICS_ANOMALY
            : assessment.deliveryDelayRisk()
                ? RecommendationType.ESCALATE_LOGISTICS
                : RecommendationType.PRIORITIZE_FULFILLMENT;

        RecommendationPriority priority = assessment.anomalyDetected()
            ? RecommendationPriority.CRITICAL
            : assessment.deliveryDelayRisk() || assessment.overdueDispatchCount() > 0
                ? RecommendationPriority.HIGH
                : RecommendationPriority.MEDIUM;

        String title = switch (type) {
            case INVESTIGATE_LOGISTICS_ANOMALY ->
                "Investigate logistics anomaly in " + task.getWarehouse().getCode();
            case ESCALATE_LOGISTICS ->
                "Escalate delivery risk for " + task.getWarehouse().getCode();
            default ->
                "Prioritize fulfillment backlog for " + task.getWarehouse().getCode();
        };

        String description = switch (type) {
            case INVESTIGATE_LOGISTICS_ANOMALY ->
                "Exceptions or repeated delivery slowdowns are stacking in " + task.getWarehouse().getName()
                    + ". Backlog is " + assessment.backlogCount()
                    + ", delayed shipments are " + assessment.delayedShipmentCount()
                    + ", and warehouse operations should investigate the blocked lane now.";
            case ESCALATE_LOGISTICS ->
                "Delivery pressure is rising in " + task.getWarehouse().getName()
                    + ". Active shipment " + task.getCustomerOrder().getExternalOrderId()
                    + (task.getTrackingReference() != null ? " is tracking as " + task.getTrackingReference() + "." : ".")
                    + " Review carrier performance and escalate the delayed route before customer impact spreads.";
            default ->
                "Dispatch backlog is building in " + task.getWarehouse().getName()
                    + " with " + assessment.backlogCount() + " open fulfillment tasks"
                    + (assessment.estimatedBacklogClearHours() != null
                        ? " and roughly " + String.format(java.util.Locale.US, "%.1f", assessment.estimatedBacklogClearHours())
                            + " hours to clear at the current pace."
                        : ".")
                    + " Pull warehouse labor forward or rebalance the lane now.";
        };

        Recommendation latest = recommendationRepository.findFirstByTenant_CodeIgnoreCaseAndTypeAndTitleOrderByCreatedAtDesc(
                task.getTenant() == null ? null : task.getTenant().getCode(),
                type,
                title)
            .orElse(null);
        if (latest != null
            && latest.getDescription().equals(description)
            && latest.getCreatedAt().isAfter(Instant.now().minusSeconds(1800))) {
            return latest;
        }

        Recommendation recommendation = recommendationRepository.save(Recommendation.builder()
            .tenant(task.getTenant())
            .type(type)
            .title(title)
            .description(description)
            .priority(priority)
            .build());

        businessEventService.record(
            BusinessEventType.RECOMMENDATION_GENERATED,
            source,
            "Generated " + priority + " logistics recommendation for " + task.getWarehouse().getCode()
        );
        return recommendation;
    }

    private Optional<TransferPlan> findTransferPlan(Inventory inventory) {
        long shortfall = inventory.getReorderThreshold() - inventory.getQuantityAvailable();
        if (shortfall <= 0) {
            return Optional.empty();
        }

        return inventoryRepository.findTransferCandidatesByTenantCode(
                inventory.getWarehouse().getTenant() == null ? null : inventory.getWarehouse().getTenant().getCode(),
                inventory.getProduct().getId(),
                inventory.getWarehouse().getId())
            .stream()
            .map(candidate -> new TransferPlan(candidate, candidate.getQuantityAvailable() - candidate.getReorderThreshold()))
            .filter(plan -> plan.transferableUnits() >= shortfall)
            .findFirst();
    }

    private String buildTitle(Inventory inventory,
                              InventoryInsight insight,
                              RecommendationPriority priority,
                              Optional<TransferPlan> transferPlan) {
        if (transferPlan.isPresent()) {
            return "Transfer stock for SKU " + inventory.getProduct().getSku()
                + " from " + transferPlan.get().sourceInventory().getWarehouse().getCode()
                + " to " + inventory.getWarehouse().getCode();
        }

        if (insight.lowStock() && priority == RecommendationPriority.CRITICAL) {
            return "Urgent reorder for SKU " + inventory.getProduct().getSku() + " at " + inventory.getWarehouse().getCode();
        }

        if (insight.depletionRisk() && !insight.lowStock()) {
            return "Prepare replenishment for SKU " + inventory.getProduct().getSku() + " at " + inventory.getWarehouse().getCode();
        }

        return "Reorder stock for SKU " + inventory.getProduct().getSku() + " at " + inventory.getWarehouse().getCode();
    }

    private String buildDescription(Inventory inventory,
                                    InventoryInsight insight,
                                    RecommendationPriority priority,
                                    StockPrediction prediction,
                                    Optional<TransferPlan> transferPlan) {
        if (transferPlan.isPresent()) {
            TransferPlan plan = transferPlan.get();
            long shortfall = inventory.getReorderThreshold() - inventory.getQuantityAvailable();
            long suggestedTransferUnits = Math.min(shortfall, plan.transferableUnits());

            String base = "Transfer " + suggestedTransferUnits + " units from "
                + plan.sourceInventory().getWarehouse().getName()
                + " to " + inventory.getWarehouse().getName()
                + " to restore the receiving warehouse to its threshold.";

            if (prediction.hoursToStockout() == null) {
                return base + " This route uses existing network surplus before placing a new purchase order.";
            }

            return base + " Current demand would otherwise exhaust stock in "
                + String.format(java.util.Locale.US, "%.1f", prediction.hoursToStockout())
                + " hours.";
        }

        if (insight.depletionRisk() && !insight.lowStock()) {
            if (prediction.hoursToStockout() == null) {
                return "Demand is accelerating for " + inventory.getWarehouse().getName()
                    + ". Review reorder settings and stage replenishment before current buffer is consumed.";
            }

            return "Demand is accelerating for " + inventory.getWarehouse().getName()
                + ". Estimated stockout window is "
                + String.format(java.util.Locale.US, "%.1f", prediction.hoursToStockout())
                + " hours at the current demand rate. Review threshold settings and stage replenishment now.";
        }

        String base = priority == RecommendationPriority.CRITICAL
            ? "Reorder immediately for " + inventory.getWarehouse().getName()
            : "Plan replenishment for " + inventory.getWarehouse().getName();

        if (prediction.hoursToStockout() == null) {
            return base + ". Current quantity is " + inventory.getQuantityAvailable()
                + " units versus a threshold of " + inventory.getReorderThreshold() + ".";
        }

        return base + ". Estimated stockout window is "
            + String.format(java.util.Locale.US, "%.1f", prediction.hoursToStockout())
            + " hours at the current demand rate.";
    }

    private record TransferPlan(Inventory sourceInventory, long transferableUnits) {
    }
}
