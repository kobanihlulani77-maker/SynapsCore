package com.synapsecore.intelligence;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.alert.AlertService;
import com.synapsecore.decision.RecommendationService;
import com.synapsecore.domain.entity.AlertSeverity;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.prediction.StockPrediction;
import com.synapsecore.prediction.StockPredictionService;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class InventoryMonitoringServiceTest {

    @Test
    void recommendationPersistenceFailureDoesNotSuppressAuthoritativeAlertEvaluation() {
        Tenant tenant = Tenant.builder().code("RECOVERY-TEST").name("Recovery Test").build();
        Warehouse warehouse = Warehouse.builder().tenant(tenant).code("WH-NORTH").name("North").location("North").build();
        Product product = Product.builder().tenant(tenant).catalogSku("SKU-RECOVERY").name("Recovery Item").category("Test").build();
        Inventory inventory = Inventory.builder()
            .warehouse(warehouse).product(product).quantityOnHand(2L).quantityAvailable(2L)
            .quantityReserved(0L).reorderThreshold(10L).build();
        StockPrediction prediction = new StockPrediction(0, 0, null, false, false, false);
        InventoryInsight insight = new InventoryInsight(true, false, false, false,
            AlertSeverity.HIGH, "HIGH", "Low stock");
        AtomicReference<com.synapsecore.domain.entity.Recommendation> alertRecommendation = new AtomicReference<>();

        StockPredictionService predictionService = new StockPredictionService(null, null) {
            @Override
            public StockPrediction estimate(Inventory ignored) {
                return prediction;
            }
        };
        InventoryIntelligenceService intelligenceService = new InventoryIntelligenceService(null) {
            @Override
            public InventoryInsight evaluate(Inventory ignored, StockPrediction ignoredPrediction) {
                return insight;
            }
        };
        RecommendationService recommendationService = new RecommendationService(null, null, null, null) {
            @Override
            public com.synapsecore.domain.entity.Recommendation createForInventory(
                Inventory ignored, InventoryInsight ignoredInsight, StockPrediction ignoredPrediction, String ignoredSource) {
                throw new IllegalStateException("simulated advisory persistence failure");
            }
        };
        AlertService alertService = new AlertService(null, null, null, null) {
            @Override
            public void syncInventoryAlerts(Inventory ignoredInventory,
                                             InventoryInsight ignoredInsight,
                                             StockPrediction ignoredPrediction,
                                             com.synapsecore.domain.entity.Recommendation recommendation,
                                             String ignoredSource) {
                alertRecommendation.set(recommendation);
            }
        };

        InventoryMonitoringService service = new InventoryMonitoringService(
            predictionService, intelligenceService, recommendationService, alertService);

        service.evaluateAfterChange(inventory, "phase2-test");

        assertThat(alertRecommendation.get()).isNull();
    }
}
