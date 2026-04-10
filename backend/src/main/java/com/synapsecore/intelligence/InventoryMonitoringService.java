package com.synapsecore.intelligence;

import com.synapsecore.alert.AlertService;
import com.synapsecore.decision.RecommendationService;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.prediction.StockPrediction;
import com.synapsecore.prediction.StockPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryMonitoringService {

    private final StockPredictionService stockPredictionService;
    private final InventoryIntelligenceService inventoryIntelligenceService;
    private final RecommendationService recommendationService;
    private final AlertService alertService;

    public void evaluateAfterChange(Inventory inventory, String source) {
        StockPrediction prediction = stockPredictionService.estimate(inventory);
        InventoryInsight insight = inventoryIntelligenceService.evaluate(inventory, prediction);
        var recommendation = recommendationService.createForInventory(inventory, insight, prediction, source);
        alertService.syncInventoryAlerts(inventory, insight, prediction, recommendation, source);
    }
}
