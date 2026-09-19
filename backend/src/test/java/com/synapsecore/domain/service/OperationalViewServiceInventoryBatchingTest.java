package com.synapsecore.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.TenantOperationalPolicy;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.intelligence.InventoryInsight;
import com.synapsecore.intelligence.InventoryIntelligenceService;
import com.synapsecore.prediction.StockPrediction;
import com.synapsecore.prediction.StockPredictionService;
import com.synapsecore.tenant.TenantContextService;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OperationalViewServiceInventoryBatchingTest {

    @Test
    void inventoryOverviewBatchesDemandAndReusesOnePolicyPerTenant() {
        Tenant tenant = tenant("DASHBOARD-BATCH");
        List<Inventory> inventories = List.of(
            inventory(31L, 21L, 11L, "SKU-ONE", "WH-NORTH", tenant),
            inventory(32L, 22L, 12L, "SKU-TWO", "WH-COAST", tenant)
        );
        AtomicInteger batchCalls = new AtomicInteger();
        AtomicInteger policyCalls = new AtomicInteger();
        AtomicInteger suppliedDemandCalls = new AtomicInteger();
        TenantOperationalPolicy policy = policy(tenant);

        StockPredictionService predictionService = new StockPredictionService(null, null) {
            @Override
            public Map<Long, Long> loadRecentUnitsByInventoryId(List<Inventory> input, Instant since) {
                batchCalls.incrementAndGet();
                assertThat(input).containsExactlyElementsOf(inventories);
                return Map.of(31L, 8L, 32L, 3L);
            }

            @Override
            public StockPrediction estimate(Inventory inventory,
                                             TenantOperationalPolicy suppliedPolicy,
                                             long recentUnits) {
                suppliedDemandCalls.incrementAndGet();
                assertThat(suppliedPolicy).isSameAs(policy);
                return prediction(recentUnits);
            }

            @Override
            public StockPrediction estimate(Inventory inventory, TenantOperationalPolicy suppliedPolicy) {
                throw new AssertionError("Successful batch path must not query demand per inventory");
            }
        };
        InventoryIntelligenceService intelligenceService = intelligenceService(policy);
        TenantOperationalPolicyService policyService = new TenantOperationalPolicyService(null, null, null) {
            @Override
            public TenantOperationalPolicy getPolicy(String tenantCode) {
                policyCalls.incrementAndGet();
                assertThat(tenantCode).isEqualTo("DASHBOARD-BATCH");
                return policy;
            }
        };

        var responses = service(inventories, predictionService, intelligenceService, policyService)
            .getInventoryOverview();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(response -> response.unitsPerHour())
            .containsExactly(8.0, 3.0);
        assertThat(batchCalls).hasValue(1);
        assertThat(policyCalls).hasValue(1);
        assertThat(suppliedDemandCalls).hasValue(2);
    }

    @Test
    void inventoryOverviewFallsBackToPerInventoryDemandWithoutRepeatingPolicyLookup() {
        Tenant tenant = tenant("DASHBOARD-FALLBACK");
        List<Inventory> inventories = List.of(
            inventory(41L, 31L, 21L, "SKU-ONE", "WH-NORTH", tenant),
            inventory(42L, 32L, 22L, "SKU-TWO", "WH-COAST", tenant)
        );
        AtomicInteger fallbackCalls = new AtomicInteger();
        AtomicInteger policyCalls = new AtomicInteger();
        TenantOperationalPolicy policy = policy(tenant);

        StockPredictionService predictionService = new StockPredictionService(null, null) {
            @Override
            public Map<Long, Long> loadRecentUnitsByInventoryId(List<Inventory> input, Instant since) {
                throw new IllegalStateException("simulated grouped read failure");
            }

            @Override
            public StockPrediction estimate(Inventory inventory, TenantOperationalPolicy suppliedPolicy) {
                fallbackCalls.incrementAndGet();
                assertThat(suppliedPolicy).isSameAs(policy);
                return prediction(inventory.getId() == 41L ? 6L : 2L);
            }

            @Override
            public StockPrediction estimate(Inventory inventory,
                                             TenantOperationalPolicy suppliedPolicy,
                                             long recentUnits) {
                throw new AssertionError("Failed batch path must use the bounded per-inventory fallback");
            }
        };
        InventoryIntelligenceService intelligenceService = intelligenceService(policy);
        TenantOperationalPolicyService policyService = new TenantOperationalPolicyService(null, null, null) {
            @Override
            public TenantOperationalPolicy getPolicy(String tenantCode) {
                policyCalls.incrementAndGet();
                return policy;
            }
        };

        var responses = service(inventories, predictionService, intelligenceService, policyService)
            .getInventoryOverview();

        assertThat(responses).extracting(response -> response.unitsPerHour())
            .containsExactly(6.0, 2.0);
        assertThat(fallbackCalls).hasValue(2);
        assertThat(policyCalls).hasValue(1);
    }

    private OperationalViewService service(List<Inventory> inventories,
                                           StockPredictionService predictionService,
                                           InventoryIntelligenceService intelligenceService,
                                           TenantOperationalPolicyService policyService) {
        InventoryRepository inventoryRepository = (InventoryRepository) Proxy.newProxyInstance(
            InventoryRepository.class.getClassLoader(),
            new Class<?>[]{InventoryRepository.class},
            (proxy, method, arguments) -> {
                if (method.getName().equals("findAllWithProductAndWarehouseByTenantCode")) {
                    return inventories;
                }
                throw new AssertionError("Unexpected InventoryRepository call: " + method.getName());
            }
        );
        TenantContextService tenantContextService = new TenantContextService(null, null, null, null, null) {
            @Override
            public String getCurrentTenantCodeOrDefault() {
                return inventories.getFirst().getTenant().getCode();
            }
        };

        return new OperationalViewService(
            null,
            null,
            inventoryRepository,
            null,
            predictionService,
            intelligenceService,
            policyService,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            tenantContextService,
            null
        );
    }

    private InventoryIntelligenceService intelligenceService(TenantOperationalPolicy policy) {
        return new InventoryIntelligenceService(null) {
            @Override
            public InventoryInsight evaluate(Inventory inventory,
                                             StockPrediction prediction,
                                             TenantOperationalPolicy suppliedPolicy) {
                assertThat(suppliedPolicy).isSameAs(policy);
                return new InventoryInsight(
                    false,
                    false,
                    false,
                    prediction.rapidConsumption(),
                    com.synapsecore.domain.entity.AlertSeverity.MEDIUM,
                    "stable",
                    "Stable"
                );
            }

            @Override
            public InventoryInsight evaluate(Inventory inventory, StockPrediction prediction) {
                throw new AssertionError("Overview must reuse the already resolved policy");
            }
        };
    }

    private StockPrediction prediction(long recentUnits) {
        return new StockPrediction(recentUnits, recentUnits, null, false, false, recentUnits >= 5L);
    }

    private Tenant tenant(String code) {
        return Tenant.builder().id(1L).code(code).name(code).build();
    }

    private TenantOperationalPolicy policy(Tenant tenant) {
        return TenantOperationalPolicy.builder()
            .tenant(tenant)
            .depletionRiskHoursThreshold(8.0)
            .urgentDepletionRiskHoursThreshold(4.0)
            .rapidConsumptionUnitsMinimum(5)
            .rapidConsumptionThresholdRatio(0.5)
            .build();
    }

    private Inventory inventory(Long id,
                                Long productId,
                                Long warehouseId,
                                String sku,
                                String warehouseCode,
                                Tenant tenant) {
        Product product = Product.builder()
            .id(productId)
            .tenant(tenant)
            .catalogSku(sku)
            .name(sku)
            .category("Test")
            .build();
        Warehouse warehouse = Warehouse.builder()
            .id(warehouseId)
            .tenant(tenant)
            .code(warehouseCode)
            .name(warehouseCode)
            .location("Test")
            .build();
        return Inventory.builder()
            .id(id)
            .tenant(tenant)
            .product(product)
            .warehouse(warehouse)
            .quantityAvailable(20L)
            .quantityOnHand(20L)
            .quantityReserved(0L)
            .quantityInbound(0L)
            .reorderThreshold(10L)
            .updatedAt(Instant.parse("2026-09-19T00:00:00Z"))
            .build();
    }
}
