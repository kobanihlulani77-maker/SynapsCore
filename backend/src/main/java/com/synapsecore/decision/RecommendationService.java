package com.synapsecore.decision;

import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.entity.RecommendationPriority;
import com.synapsecore.domain.entity.RecommendationStatus;
import com.synapsecore.domain.entity.RecommendationType;
import com.synapsecore.domain.entity.TenantOperationalPolicy;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.service.TenantOperationalPolicyService;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.fulfillment.FulfillmentAssessment;
import com.synapsecore.intelligence.InventoryInsight;
import com.synapsecore.prediction.StockPrediction;
import com.synapsecore.scenario.dto.ScenarioRecommendationProjection;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final InventoryRepository inventoryRepository;
    private final RecommendationRepository recommendationRepository;
    private final BusinessEventService businessEventService;
    private final TenantOperationalPolicyService tenantOperationalPolicyService;
    private static final ConcurrentHashMap<String, ReentrantLock> CONDITION_LOCKS = new ConcurrentHashMap<>();

    public Recommendation createForInventory(Inventory inventory, InventoryInsight insight, StockPrediction prediction, String source) {
        String conditionKey = inventoryConditionKey(inventory);
        ReentrantLock lock = CONDITION_LOCKS.computeIfAbsent(conditionKey, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return createForInventoryLocked(inventory, insight, prediction, source);
        } finally {
            lock.unlock();
            CONDITION_LOCKS.remove(conditionKey, lock);
        }
    }

    private Recommendation createForInventoryLocked(Inventory inventory, InventoryInsight insight, StockPrediction prediction, String source) {
        ScenarioRecommendationProjection projection = previewForInventory(inventory, insight, prediction);
        String tenantCode = inventory.getWarehouse().getTenant().getCode();
        String sourceRef = inventorySourceRef(inventory);
        String conditionKey = inventoryConditionKey(inventory);
        Recommendation existing = recommendationRepository.findByTenantCodeAndConditionKeyForUpdate(
            tenantCode, conditionKey, RecommendationStatus.CURRENT).orElse(null);
        if (projection == null) {
            Recommendation retired = retire(existing);
            retireInvalidatedTransfers(inventory);
            return retired;
        }

        String policyExplanation = buildInventoryPolicyExplanation(inventory, insight, prediction, projection.priority());
        Optional<TransferPlan> transferPlan = projection.type() == RecommendationType.TRANSFER_STOCK
            ? findTransferPlan(inventory)
            : Optional.empty();
        Recommendation recommendation = existing == null ? Recommendation.builder()
            .tenant(inventory.getWarehouse().getTenant())
            .warehouse(inventory.getWarehouse())
            .product(inventory.getProduct())
            .sourceType("INVENTORY")
            .sourceRef(sourceRef)
            .conditionKey(conditionKey)
            .build() : existing;
        recommendation.setTenant(inventory.getWarehouse().getTenant());
        recommendation.setWarehouse(inventory.getWarehouse());
        recommendation.setProduct(inventory.getProduct());
        recommendation.setSourceWarehouse(transferPlan.map(plan -> plan.sourceInventory().getWarehouse()).orElse(null));
        recommendation.setDestinationWarehouse(transferPlan.map(plan -> inventory.getWarehouse()).orElse(null));
        recommendation.setSuggestedQuantity(transferPlan.map(plan -> Math.min(
            inventory.getReorderThreshold() - inventory.getQuantityAvailable(), plan.transferableUnits())).orElse(null));
        recommendation.setSourceType("INVENTORY");
        recommendation.setSourceRef(sourceRef);
        recommendation.setConditionKey(conditionKey);
        recommendation.setType(projection.type());
        recommendation.setTitle(projection.title());
        recommendation.setDescription(projection.description());
        recommendation.setPolicyExplanation(policyExplanation);
        recommendation.setPriority(projection.priority());
        recommendation.setStatus(RecommendationStatus.CURRENT);
        Recommendation saved = recommendationRepository.save(recommendation);
        if (existing == null) {
            businessEventService.record(
                BusinessEventType.RECOMMENDATION_GENERATED,
                source,
                "Generated " + projection.priority() + " recommendation for " + inventory.getProduct().resolveCatalogSku()
                    + " in " + inventory.getWarehouse().getCode()
            );
        }
        retireInvalidatedTransfers(inventory);
        return saved;
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

        TenantOperationalPolicy policy = tenantOperationalPolicyService.getPolicy(
            inventory.getTenant() != null
                ? inventory.getTenant().getCode()
                : inventory.getWarehouse().getTenant().getCode()
        );
        RecommendationPriority priority;
        if (insight.lowStock()) {
            priority = insight.elevatedUrgency()
                ? policy.getCriticalLowStockRecommendationPriority()
                : prediction.depletionRisk() ? policy.getUrgentDepletionRiskRecommendationPriority() : policy.getLowStockRecommendationPriority();
        } else {
            priority = prediction.urgentRisk()
                ? policy.getUrgentDepletionRiskRecommendationPriority()
                : policy.getDepletionRiskRecommendationPriority();
        }

        RecommendationType type = transferPlan.isPresent()
            ? RecommendationType.TRANSFER_STOCK
            : insight.lowStock() && insight.elevatedUrgency()
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
        String conditionKey = fulfillmentConditionKey(task);
        ReentrantLock lock = CONDITION_LOCKS.computeIfAbsent(conditionKey, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return createForFulfillmentLocked(task, assessment, source);
        } finally {
            lock.unlock();
            CONDITION_LOCKS.remove(conditionKey, lock);
        }
    }

    private Recommendation createForFulfillmentLocked(FulfillmentTask task, FulfillmentAssessment assessment, String source) {
        String tenantCode = task.getTenant().getCode();
        String sourceRef = fulfillmentSourceRef(task);
        String conditionKey = fulfillmentConditionKey(task);
        Recommendation existing = recommendationRepository.findByTenantCodeAndConditionKeyForUpdate(
            tenantCode, conditionKey, RecommendationStatus.CURRENT).orElse(null);
        if (!assessment.backlogRisk() && !assessment.deliveryDelayRisk() && !assessment.anomalyDetected()) {
            return retire(existing);
        }

        RecommendationType type = assessment.anomalyDetected()
            ? RecommendationType.INVESTIGATE_LOGISTICS_ANOMALY
            : assessment.deliveryDelayRisk()
                ? RecommendationType.ESCALATE_LOGISTICS
                : RecommendationType.PRIORITIZE_FULFILLMENT;

        TenantOperationalPolicy policy = tenantOperationalPolicyService.getPolicy(task.getTenant().getCode());
        RecommendationPriority priority = assessment.anomalyDetected()
            ? policy.getFulfillmentAnomalyRecommendationPriority()
            : assessment.deliveryDelayRisk() || assessment.overdueDispatchCount() > 0
                ? policy.getDeliveryDelayRecommendationPriority()
                : policy.getBacklogRecommendationPriority();

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

        String policyExplanation = buildFulfillmentPolicyExplanation(assessment, priority, policy);
        Recommendation recommendation = existing == null ? Recommendation.builder()
            .tenant(task.getTenant())
            .warehouse(task.getWarehouse())
            .sourceType("FULFILLMENT")
            .sourceRef(sourceRef)
            .conditionKey(conditionKey)
            .build() : existing;
        recommendation.setTenant(task.getTenant());
        recommendation.setWarehouse(task.getWarehouse());
        recommendation.setProduct(null);
        recommendation.setSourceWarehouse(null);
        recommendation.setDestinationWarehouse(null);
        recommendation.setSuggestedQuantity(null);
        recommendation.setSourceType("FULFILLMENT");
        recommendation.setSourceRef(sourceRef);
        recommendation.setConditionKey(conditionKey);
        recommendation.setType(type);
        recommendation.setTitle(title);
        recommendation.setDescription(description);
        recommendation.setPolicyExplanation(policyExplanation);
        recommendation.setPriority(priority);
        recommendation.setStatus(RecommendationStatus.CURRENT);
        Recommendation saved = recommendationRepository.save(recommendation);
        if (existing == null) {
            businessEventService.record(
                BusinessEventType.RECOMMENDATION_GENERATED,
                source,
                "Generated " + priority + " logistics recommendation for " + task.getWarehouse().getCode()
            );
        }
        return saved;
    }

    private Recommendation retire(Recommendation recommendation) {
        if (recommendation == null) {
            return null;
        }
        recommendation.setStatus(RecommendationStatus.RETIRED);
        return recommendationRepository.save(recommendation);
    }

    private void retireInvalidatedTransfers(Inventory changedInventory) {
        String tenantCode = changedInventory.getWarehouse().getTenant().getCode();
        recommendationRepository.findAllByTenant_CodeIgnoreCaseAndTypeAndProduct_IdAndSourceWarehouse_IdAndStatus(
                tenantCode,
                RecommendationType.TRANSFER_STOCK,
                changedInventory.getProduct().getId(),
                changedInventory.getWarehouse().getId(),
                RecommendationStatus.CURRENT)
            .stream()
            .filter(recommendation -> !transferStillValid(recommendation, changedInventory))
            .forEach(this::retire);
    }

    private boolean transferStillValid(Recommendation recommendation, Inventory sourceInventory) {
        if (recommendation.getDestinationWarehouse() == null || recommendation.getProduct() == null) {
            return false;
        }
        return inventoryRepository.findByProductIdAndWarehouseId(
                recommendation.getProduct().getId(), recommendation.getDestinationWarehouse().getId())
            .filter(destination -> destination.getQuantityAvailable() <= destination.getReorderThreshold())
            .map(destination -> {
                long shortfall = destination.getReorderThreshold() - destination.getQuantityAvailable();
                long transferable = sourceInventory.getQuantityAvailable() - sourceInventory.getReorderThreshold();
                return shortfall > 0 && transferable >= shortfall;
            })
            .orElse(false);
    }

    private String inventorySourceRef(Inventory inventory) {
        return "inventory:" + inventory.getProduct().getId() + ":" + inventory.getWarehouse().getId();
    }

    private String inventoryConditionKey(Inventory inventory) {
        return "INVENTORY|" + inventory.getProduct().getId() + "|" + inventory.getWarehouse().getId();
    }

    private String fulfillmentSourceRef(FulfillmentTask task) {
        return "fulfillment:" + task.getWarehouse().getId();
    }

    private String fulfillmentConditionKey(FulfillmentTask task) {
        return "FULFILLMENT|" + task.getWarehouse().getId();
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

    private String buildInventoryPolicyExplanation(Inventory inventory,
                                                   InventoryInsight insight,
                                                   StockPrediction prediction,
                                                   RecommendationPriority priority) {
        TenantOperationalPolicy policy = tenantOperationalPolicyService.getPolicy(
            inventory.getTenant() != null
                ? inventory.getTenant().getCode()
                : inventory.getWarehouse().getTenant().getCode()
        );
        if (insight.lowStock()) {
            return "Tenant policy selected " + priority + " because available stock is "
                + inventory.getQuantityAvailable() + " against threshold " + inventory.getReorderThreshold()
                + " with critical ratio " + policy.getLowStockCriticalRatio()
                + (prediction.hoursToStockout() == null
                    ? "."
                    : " and estimated stockout in " + String.format(java.util.Locale.US, "%.1f", prediction.hoursToStockout())
                        + " hours.");
        }
        return "Tenant policy selected " + priority + " because estimated stockout is "
            + (prediction.hoursToStockout() == null
                ? "unknown"
                : String.format(java.util.Locale.US, "%.1f hours", prediction.hoursToStockout()))
            + " with depletion threshold " + policy.getDepletionRiskHoursThreshold()
            + " hours and urgent threshold " + policy.getUrgentDepletionRiskHoursThreshold() + " hours.";
    }

    private String buildFulfillmentPolicyExplanation(FulfillmentAssessment assessment,
                                                     RecommendationPriority priority,
                                                     TenantOperationalPolicy policy) {
        return "Tenant policy selected " + priority
            + " with backlog=" + assessment.backlogCount()
            + " (risk threshold " + policy.getBacklogRiskCount()
            + ", critical threshold " + policy.getBacklogCriticalCount()
            + "), delayed shipments=" + assessment.delayedShipmentCount()
            + " (threshold " + policy.getDelayedShipmentCountThreshold()
            + "), and overdue dispatch=" + assessment.overdueDispatchCount()
            + " (threshold " + policy.getOverdueDispatchCountThreshold() + ").";
    }

    private String buildTitle(Inventory inventory,
                              InventoryInsight insight,
                              RecommendationPriority priority,
                              Optional<TransferPlan> transferPlan) {
        if (transferPlan.isPresent()) {
            return "Transfer stock for SKU " + inventory.getProduct().resolveCatalogSku()
                + " from " + transferPlan.get().sourceInventory().getWarehouse().getCode()
                + " to " + inventory.getWarehouse().getCode();
        }

        if (insight.lowStock() && priority == RecommendationPriority.CRITICAL) {
            return "Urgent reorder for SKU " + inventory.getProduct().resolveCatalogSku() + " at " + inventory.getWarehouse().getCode();
        }

        if (insight.depletionRisk() && !insight.lowStock()) {
            return "Prepare replenishment for SKU " + inventory.getProduct().resolveCatalogSku() + " at " + inventory.getWarehouse().getCode();
        }

        return "Reorder stock for SKU " + inventory.getProduct().resolveCatalogSku() + " at " + inventory.getWarehouse().getCode();
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
