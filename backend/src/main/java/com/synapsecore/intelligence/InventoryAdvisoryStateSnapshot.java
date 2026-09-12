package com.synapsecore.intelligence;

import com.synapsecore.domain.entity.AlertType;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.entity.RecommendationType;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Current advisory identities used to avoid transaction work for proven no-op
 * inventory reconciliation rows. Active or previously persisted conditions
 * continue through the authoritative locking services.
 */
public record InventoryAdvisoryStateSnapshot(
    Set<String> currentRecommendationConditionKeys,
    Set<String> currentTransferSourceKeys,
    Set<String> activeAlertConditionKeys,
    boolean forceSynchronization
) {

    public InventoryAdvisoryStateSnapshot {
        currentRecommendationConditionKeys = Set.copyOf(currentRecommendationConditionKeys);
        currentTransferSourceKeys = Set.copyOf(currentTransferSourceKeys);
        activeAlertConditionKeys = Set.copyOf(activeAlertConditionKeys);
    }

    public static InventoryAdvisoryStateSnapshot from(
        Collection<Recommendation> currentRecommendations,
        Collection<String> activeAlertConditionKeys
    ) {
        Set<String> recommendationKeys = new LinkedHashSet<>();
        Set<String> transferSourceKeys = new LinkedHashSet<>();
        for (Recommendation recommendation : currentRecommendations) {
            if (recommendation.getConditionKey() != null) {
                recommendationKeys.add(recommendation.getConditionKey());
            }
            if (recommendation.getType() == RecommendationType.TRANSFER_STOCK
                && recommendation.getProduct() != null
                && recommendation.getSourceWarehouse() != null) {
                transferSourceKeys.add(transferSourceKey(
                    recommendation.getProduct().getId(),
                    recommendation.getSourceWarehouse().getId()
                ));
            }
        }
        return new InventoryAdvisoryStateSnapshot(
            recommendationKeys,
            transferSourceKeys,
            activeAlertConditionKeys.stream()
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet()),
            false
        );
    }

    public static InventoryAdvisoryStateSnapshot fullSynchronization() {
        return new InventoryAdvisoryStateSnapshot(Set.of(), Set.of(), Set.of(), true);
    }

    public boolean requiresRecommendationSynchronization(Inventory inventory, InventoryInsight insight) {
        return forceSynchronization
            || insight.lowStock()
            || insight.depletionRisk()
            || currentRecommendationConditionKeys.contains(recommendationConditionKey(inventory))
            || currentTransferSourceKeys.contains(transferSourceKey(
                inventory.getProduct().getId(),
                inventory.getWarehouse().getId()
            ));
    }

    public boolean requiresAlertSynchronization(Inventory inventory, InventoryInsight insight) {
        return forceSynchronization
            || insight.lowStock()
            || insight.depletionRisk()
            || activeAlertConditionKeys.contains(alertConditionKey(AlertType.LOW_STOCK, inventory))
            || activeAlertConditionKeys.contains(alertConditionKey(AlertType.DEPLETION_RISK, inventory));
    }

    public static String recommendationConditionKey(Inventory inventory) {
        return "INVENTORY|" + inventory.getProduct().getId() + "|" + inventory.getWarehouse().getId();
    }

    public static String alertConditionKey(AlertType type, Inventory inventory) {
        return type.name() + "|PRODUCT:" + inventory.getProduct().getId()
            + "|WAREHOUSE:" + inventory.getWarehouse().getId();
    }

    private static String transferSourceKey(Long productId, Long warehouseId) {
        return productId + "|" + warehouseId;
    }
}
