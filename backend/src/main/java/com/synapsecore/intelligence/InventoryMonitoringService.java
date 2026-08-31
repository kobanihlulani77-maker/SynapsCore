package com.synapsecore.intelligence;

import com.synapsecore.alert.AlertService;
import com.synapsecore.decision.RecommendationService;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.prediction.StockPrediction;
import com.synapsecore.prediction.StockPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryMonitoringService {

    private final StockPredictionService stockPredictionService;
    private final InventoryIntelligenceService inventoryIntelligenceService;
    private final RecommendationService recommendationService;
    private final AlertService alertService;

    @Transactional
    public void evaluateAfterChange(Inventory inventory, String source) {
        StockPrediction prediction = stockPredictionService.estimate(inventory);
        InventoryInsight insight = inventoryIntelligenceService.evaluate(inventory, prediction);
        com.synapsecore.domain.entity.Recommendation recommendation = null;
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
        alertService.syncInventoryAlerts(inventory, insight, prediction, recommendation, source);
    }
}
