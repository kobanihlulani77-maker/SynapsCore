package com.synapsecore.intelligence;

import com.synapsecore.alert.AlertService;
import com.synapsecore.decision.RecommendationService;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.TenantOperationalPolicy;
import com.synapsecore.prediction.StockPrediction;
import com.synapsecore.prediction.StockPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryMonitoringService {

    private final StockPredictionService stockPredictionService;
    private final InventoryIntelligenceService inventoryIntelligenceService;
    private final RecommendationService recommendationService;
    private final AlertService alertService;

    public void evaluateAfterChange(Inventory inventory, String source) {
        StockPrediction prediction = stockPredictionService.estimate(inventory);
        InventoryInsight insight = inventoryIntelligenceService.evaluate(inventory, prediction);
        persistAdvisoryState(inventory, source, prediction, insight);
    }

    public void evaluateAfterChange(Inventory inventory,
                                    String source,
                                    TenantOperationalPolicy policy) {
        StockPrediction prediction = stockPredictionService.estimate(inventory, policy);
        InventoryInsight insight = inventoryIntelligenceService.evaluate(inventory, prediction, policy);
        persistAdvisoryState(inventory, source, prediction, insight);
    }

    public void evaluateAfterChange(Inventory inventory,
                                    String source,
                                    TenantOperationalPolicy policy,
                                    long recentUnits) {
        StockPrediction prediction = stockPredictionService.estimate(inventory, policy, recentUnits);
        InventoryInsight insight = inventoryIntelligenceService.evaluate(inventory, prediction, policy);
        persistAdvisoryState(inventory, source, prediction, insight);
    }

    public void evaluateAfterChange(Inventory inventory,
                                    String source,
                                    TenantOperationalPolicy policy,
                                    InventoryAdvisoryStateSnapshot advisoryState) {
        StockPrediction prediction = stockPredictionService.estimate(inventory, policy);
        InventoryInsight insight = inventoryIntelligenceService.evaluate(inventory, prediction, policy);
        persistAdvisoryState(inventory, source, prediction, insight, advisoryState);
    }

    public void evaluateAfterChange(Inventory inventory,
                                    String source,
                                    TenantOperationalPolicy policy,
                                    long recentUnits,
                                    InventoryAdvisoryStateSnapshot advisoryState) {
        StockPrediction prediction = stockPredictionService.estimate(inventory, policy, recentUnits);
        InventoryInsight insight = inventoryIntelligenceService.evaluate(inventory, prediction, policy);
        persistAdvisoryState(inventory, source, prediction, insight, advisoryState);
    }

    private void persistAdvisoryState(Inventory inventory,
                                      String source,
                                      StockPrediction prediction,
                                      InventoryInsight insight) {
        persistAdvisoryState(
            inventory,
            source,
            prediction,
            insight,
            InventoryAdvisoryStateSnapshot.fullSynchronization()
        );
    }

    private void persistAdvisoryState(Inventory inventory,
                                      String source,
                                      StockPrediction prediction,
                                      InventoryInsight insight,
                                      InventoryAdvisoryStateSnapshot advisoryState) {
        com.synapsecore.domain.entity.Recommendation recommendation = null;
        if (advisoryState.requiresRecommendationSynchronization(inventory, insight)) {
            try {
                recommendation = recommendationService.createForInventory(inventory, insight, prediction, source);
            } catch (RuntimeException exception) {
                log.warn("Recommendation evaluation failed for tenant {} warehouse {} product {} from {}: {}",
                    inventory.getWarehouse().getTenant().getCode(),
                    inventory.getWarehouse().getCode(),
                    inventory.getProduct().resolveCatalogSku(),
                    source,
                    exception.getMessage());
            }
        }
        if (advisoryState.requiresAlertSynchronization(inventory, insight)) {
            alertService.syncInventoryAlerts(inventory, insight, prediction, recommendation, source);
        }
    }
}
