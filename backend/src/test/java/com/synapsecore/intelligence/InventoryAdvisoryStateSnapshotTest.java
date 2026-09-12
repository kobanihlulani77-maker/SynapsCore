package com.synapsecore.intelligence;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.domain.entity.AlertSeverity;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.entity.RecommendationType;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.Warehouse;
import java.util.List;
import org.junit.jupiter.api.Test;

class InventoryAdvisoryStateSnapshotTest {

    @Test
    void activeConditionsAndFallbackAlwaysUseTheAuthoritativeSynchronizationPath() {
        Inventory inventory = inventory(2L, 3L);
        InventoryInsight lowStock = new InventoryInsight(true, false, false, false,
            AlertSeverity.HIGH, "HIGH", "Low stock");
        InventoryInsight healthy = new InventoryInsight(false, false, false, false,
            AlertSeverity.MEDIUM, "HEALTHY", "Healthy stock");
        InventoryAdvisoryStateSnapshot empty = InventoryAdvisoryStateSnapshot.from(List.of(), List.of());

        assertThat(empty.requiresRecommendationSynchronization(inventory, lowStock)).isTrue();
        assertThat(empty.requiresAlertSynchronization(inventory, lowStock)).isTrue();
        assertThat(InventoryAdvisoryStateSnapshot.fullSynchronization()
            .requiresRecommendationSynchronization(inventory, healthy)).isTrue();
        assertThat(InventoryAdvisoryStateSnapshot.fullSynchronization()
            .requiresAlertSynchronization(inventory, healthy)).isTrue();
    }

    @Test
    void currentTransferSourceKeepsStableSourceInventoryInTheRepairPath() {
        Tenant tenant = Tenant.builder().id(1L).code("SNAPSHOT").name("Snapshot").build();
        Product product = Product.builder().id(2L).tenant(tenant)
            .catalogSku("SKU-SNAPSHOT").name("Snapshot Item").category("Test").build();
        Warehouse source = Warehouse.builder().id(3L).tenant(tenant)
            .code("WH-SOURCE").name("Source").location("Source").build();
        Warehouse destination = Warehouse.builder().id(4L).tenant(tenant)
            .code("WH-DEST").name("Destination").location("Destination").build();
        Inventory sourceInventory = Inventory.builder().id(5L).tenant(tenant)
            .product(product).warehouse(source).quantityAvailable(20L).quantityOnHand(20L)
            .quantityReserved(0L).reorderThreshold(10L).build();
        Recommendation transfer = Recommendation.builder()
            .product(product)
            .sourceWarehouse(source)
            .destinationWarehouse(destination)
            .type(RecommendationType.TRANSFER_STOCK)
            .conditionKey("INVENTORY|2|4")
            .build();
        InventoryInsight healthy = new InventoryInsight(false, false, false, false,
            AlertSeverity.MEDIUM, "HEALTHY", "Healthy stock");

        InventoryAdvisoryStateSnapshot snapshot = InventoryAdvisoryStateSnapshot.from(
            List.of(transfer),
            List.of()
        );

        assertThat(snapshot.requiresRecommendationSynchronization(sourceInventory, healthy)).isTrue();
        assertThat(snapshot.requiresAlertSynchronization(sourceInventory, healthy)).isFalse();
    }

    private Inventory inventory(Long productId, Long warehouseId) {
        Tenant tenant = Tenant.builder().id(1L).code("SNAPSHOT").name("Snapshot").build();
        Product product = Product.builder().id(productId).tenant(tenant)
            .catalogSku("SKU-SNAPSHOT").name("Snapshot Item").category("Test").build();
        Warehouse warehouse = Warehouse.builder().id(warehouseId).tenant(tenant)
            .code("WH-SNAPSHOT").name("Snapshot").location("Snapshot").build();
        return Inventory.builder().id(5L).tenant(tenant)
            .product(product).warehouse(warehouse).quantityAvailable(20L).quantityOnHand(20L)
            .quantityReserved(0L).reorderThreshold(10L).build();
    }
}
