package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.decision.RecommendationService;
import com.synapsecore.domain.entity.AlertSeverity;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.entity.RecommendationPriority;
import com.synapsecore.domain.entity.RecommendationStatus;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.intelligence.InventoryInsight;
import com.synapsecore.prediction.StockPrediction;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecommendationLifecyclePhase1IntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private RecommendationService recommendationService;

    @Test
    void currentConditionRefreshesInPlaceRetiresOnClearAndCanRecur() {
        Tenant tenant = tenantRepository.save(Tenant.builder().code("REC-" + UUID.randomUUID()).name("Recommendation Test").build());
        Warehouse warehouse = warehouseRepository.save(warehouse(tenant, "WH-NORTH"));
        Product product = productRepository.save(Product.builder().tenant(tenant).catalogSku("SKU-REC-1").name("Recommendation Item").category("Test").build());
        Inventory inventory = inventoryRepository.save(Inventory.builder()
            .warehouse(warehouse)
            .product(product)
            .quantityOnHand(5L)
            .quantityReserved(0L)
            .quantityAvailable(5L)
            .reorderThreshold(10L)
            .build());

        InventoryInsight lowStock = new InventoryInsight(true, false, false, false, AlertSeverity.HIGH, "HIGH", "Low stock.");
        InventoryInsight clear = new InventoryInsight(false, false, false, false, AlertSeverity.MEDIUM, "LOW", "Stable.");
        StockPrediction prediction = new StockPrediction(0, 0, null, false, false, false);

        Recommendation first = recommendationService.createForInventory(inventory, lowStock, prediction, "phase1-test");
        assertThat(first.getStatus()).isEqualTo(RecommendationStatus.CURRENT);
        assertThat(first.getWarehouse().getCode()).isEqualTo("WH-NORTH");
        assertThat(first.getProduct().resolveCatalogSku()).isEqualTo("SKU-REC-1");

        Recommendation refreshed = recommendationService.createForInventory(inventory, lowStock, prediction, "phase1-test");
        assertThat(refreshed.getId()).isEqualTo(first.getId());
        assertThat(recommendationRepository.countByTenant_CodeIgnoreCaseAndStatus(tenant.getCode(), RecommendationStatus.CURRENT)).isEqualTo(1);

        Recommendation retired = recommendationService.createForInventory(inventory, clear, prediction, "phase1-test");
        assertThat(retired.getId()).isEqualTo(first.getId());
        assertThat(retired.getStatus()).isEqualTo(RecommendationStatus.RETIRED);
        assertThat(recommendationRepository.countByTenant_CodeIgnoreCaseAndStatus(tenant.getCode(), RecommendationStatus.CURRENT)).isZero();

        Recommendation recurrence = recommendationService.createForInventory(inventory, lowStock, prediction, "phase1-test");
        assertThat(recurrence.getId()).isNotEqualTo(first.getId());
        assertThat(recurrence.getStatus()).isEqualTo(RecommendationStatus.CURRENT);
    }

    @Test
    void transferRecommendationCarriesBothWarehousesAndProductIdentity() {
        Tenant tenant = tenantRepository.save(Tenant.builder().code("REC-" + UUID.randomUUID()).name("Transfer Test").build());
        Warehouse north = warehouseRepository.save(warehouse(tenant, "WH-NORTH"));
        Warehouse coast = warehouseRepository.save(warehouse(tenant, "WH-COAST"));
        Product product = productRepository.save(Product.builder().tenant(tenant).catalogSku("SKU-TRANSFER-1").name("Transfer Item").category("Test").build());
        Inventory destination = inventoryRepository.save(Inventory.builder()
            .warehouse(north).product(product).quantityOnHand(2L).quantityReserved(0L).quantityAvailable(2L).reorderThreshold(10L).build());
        inventoryRepository.save(Inventory.builder()
            .warehouse(coast).product(product).quantityOnHand(30L).quantityReserved(0L).quantityAvailable(30L).reorderThreshold(10L).build());

        InventoryInsight lowStock = new InventoryInsight(true, false, false, false, AlertSeverity.HIGH, "HIGH", "Low stock.");
        Recommendation recommendation = recommendationService.createForInventory(
            destination,
            lowStock,
            new StockPrediction(0, 0, null, false, false, false),
            "phase1-test");

        assertThat(recommendation.getType()).isEqualTo(com.synapsecore.domain.entity.RecommendationType.TRANSFER_STOCK);
        assertThat(recommendation.getSourceWarehouse().getCode()).isEqualTo("WH-COAST");
        assertThat(recommendation.getDestinationWarehouse().getCode()).isEqualTo("WH-NORTH");
        assertThat(recommendation.getSuggestedQuantity()).isEqualTo(8L);
        assertThat(recommendation.getProduct().resolveCatalogSku()).isEqualTo("SKU-TRANSFER-1");
    }

    private Warehouse warehouse(Tenant tenant, String code) {
        return Warehouse.builder().tenant(tenant).code(code).name(code).location(code + " location").build();
    }
}
