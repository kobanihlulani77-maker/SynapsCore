package com.synapsecore.prediction;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.TenantOperationalPolicy;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.OrderItemRepository;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class StockPredictionServiceTest {

    @Test
    void batchesRecentDemandByExactProductAndWarehouseAndSuppliesZeroForMissingRows() {
        AtomicInteger batchCalls = new AtomicInteger();
        AtomicReference<Object[]> batchArguments = new AtomicReference<>();
        OrderItemRepository repository = repository((method, arguments) -> {
            if (method.equals("sumRecentQuantityByProductAndWarehouse")
                && arguments[0] instanceof Collection<?>) {
                batchCalls.incrementAndGet();
                batchArguments.set(arguments);
                return List.of(summary(21L, 11L, 7L), summary(21L, 12L, 3L));
            }
            throw new AssertionError("Unexpected repository call: " + method);
        });
        StockPredictionService service = new StockPredictionService(repository, null);
        Tenant tenant = Tenant.builder().id(1L).code("DEMAND-TEST").name("Demand test").build();
        Warehouse north = warehouse(11L, "WH-NORTH", tenant);
        Warehouse coast = warehouse(12L, "WH-COAST", tenant);
        Product first = product(21L, "SKU-ONE", tenant);
        Product second = product(22L, "SKU-TWO", tenant);
        Inventory northFirst = inventory(31L, first, north);
        Inventory coastFirst = inventory(32L, first, coast);
        Inventory northSecond = inventory(33L, second, north);
        Instant since = Instant.parse("2026-09-12T16:00:00Z");

        Map<Long, Long> recentUnits = service.loadRecentUnitsByInventoryId(
            List.of(northFirst, coastFirst, northSecond), since);

        assertThat(recentUnits).containsExactly(
            Map.entry(31L, 7L),
            Map.entry(32L, 3L),
            Map.entry(33L, 0L)
        );
        assertThat(batchCalls).hasValue(1);
        assertThat(batchArguments.get()[0]).isEqualTo(Set.of(21L, 22L));
        assertThat(batchArguments.get()[1]).isEqualTo(Set.of(11L, 12L));
        assertThat(batchArguments.get()[2]).isEqualTo(since);
    }

    @Test
    void suppliedRecentDemandPreservesPredictionRulesWithoutAnotherRepositoryQuery() {
        OrderItemRepository repository = repository((method, arguments) -> {
            throw new AssertionError("Supplied recent demand must not query the repository: " + method);
        });
        StockPredictionService service = new StockPredictionService(repository, null);
        Tenant tenant = Tenant.builder().id(1L).code("PREDICTION-TEST").name("Prediction test").build();
        Inventory inventory = inventory(
            31L,
            product(21L, "SKU-ONE", tenant),
            warehouse(11L, "WH-NORTH", tenant)
        );
        inventory.setQuantityAvailable(20L);
        inventory.setReorderThreshold(10L);
        TenantOperationalPolicy policy = TenantOperationalPolicy.builder()
            .tenant(tenant)
            .depletionRiskHoursThreshold(4.0)
            .urgentDepletionRiskHoursThreshold(2.0)
            .rapidConsumptionUnitsMinimum(5)
            .rapidConsumptionThresholdRatio(0.5)
            .build();

        StockPrediction prediction = service.estimate(inventory, policy, 8L);

        assertThat(prediction.recentUnitsOrdered()).isEqualTo(8L);
        assertThat(prediction.unitsPerHour()).isEqualTo(8.0);
        assertThat(prediction.hoursToStockout()).isEqualTo(2.5);
        assertThat(prediction.depletionRisk()).isTrue();
        assertThat(prediction.urgentRisk()).isFalse();
        assertThat(prediction.rapidConsumption()).isTrue();
    }

    private OrderItemRepository repository(RepositoryCall call) {
        return (OrderItemRepository) Proxy.newProxyInstance(
            OrderItemRepository.class.getClassLoader(),
            new Class<?>[]{OrderItemRepository.class},
            (proxy, method, arguments) -> call.invoke(method.getName(), arguments)
        );
    }

    @FunctionalInterface
    private interface RepositoryCall {

        Object invoke(String method, Object[] arguments);
    }

    private OrderItemRepository.RecentQuantitySummary summary(Long productId,
                                                               Long warehouseId,
                                                               Long recentUnits) {
        return new OrderItemRepository.RecentQuantitySummary() {
            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public Long getWarehouseId() {
                return warehouseId;
            }

            @Override
            public Long getRecentUnits() {
                return recentUnits;
            }
        };
    }

    private Warehouse warehouse(Long id, String code, Tenant tenant) {
        return Warehouse.builder().id(id).tenant(tenant).code(code).name(code).location("Test").build();
    }

    private Product product(Long id, String sku, Tenant tenant) {
        return Product.builder().id(id).tenant(tenant).catalogSku(sku).name(sku).category("Test").build();
    }

    private Inventory inventory(Long id, Product product, Warehouse warehouse) {
        return Inventory.builder()
            .id(id)
            .tenant(warehouse.getTenant())
            .product(product)
            .warehouse(warehouse)
            .quantityAvailable(20L)
            .quantityOnHand(20L)
            .quantityReserved(0L)
            .reorderThreshold(10L)
            .build();
    }
}
