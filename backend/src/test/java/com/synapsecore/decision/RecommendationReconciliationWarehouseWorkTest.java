package com.synapsecore.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.TenantOperationalPolicy;
import com.synapsecore.domain.entity.Warehouse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RecommendationReconciliationWarehouseWorkTest {

    @Test
    void selectsOneFinalStateEquivalentTaskPerWarehouse() {
        Warehouse north = warehouse(11L, "WH-NORTH");
        Warehouse coast = warehouse(12L, "WH-COAST");
        FulfillmentTask newestNorth = task(101L, north);
        FulfillmentTask coastOnly = task(201L, coast);
        FulfillmentTask oldestNorth = task(102L, north);

        Map<Long, FulfillmentTask> representatives =
            RecommendationReconciliationService.selectOneTaskPerWarehouse(
                List.of(newestNorth, coastOnly, oldestNorth)
            );

        assertThat(representatives).hasSize(2);
        assertThat(representatives.values()).containsExactly(oldestNorth, coastOnly);
        assertThat(representatives.get(north.getId())).isSameAs(oldestNorth);
        assertThat(representatives.get(coast.getId())).isSameAs(coastOnly);
    }

    @Test
    void loadsOneInventoryPolicyPerTenantForTheWholePass() {
        Tenant northTenant = Tenant.builder().code("TENANT-NORTH").name("North tenant").build();
        Tenant coastTenant = Tenant.builder().code("TENANT-COAST").name("Coast tenant").build();
        Inventory northOne = inventory(warehouse(11L, "WH-NORTH", northTenant));
        Inventory northTwo = inventory(warehouse(12L, "WH-NORTH-2", northTenant));
        Inventory coast = inventory(warehouse(13L, "WH-COAST", coastTenant));
        AtomicInteger loads = new AtomicInteger();

        Map<String, TenantOperationalPolicy> policies = new LinkedHashMap<>();
        for (Inventory inventory : List.of(northOne, northTwo, coast)) {
            String tenantCode = inventory.getWarehouse().getTenant().getCode();
            RecommendationReconciliationService.resolveInventoryPolicy(
                policies,
                tenantCode,
                code -> {
                    loads.incrementAndGet();
                    Tenant tenant = code.equals(northTenant.getCode()) ? northTenant : coastTenant;
                    return TenantOperationalPolicy.builder().tenant(tenant).build();
                }
            );
        }

        assertThat(policies).containsOnlyKeys("TENANT-NORTH", "TENANT-COAST");
        assertThat(loads).hasValue(2);
    }

    private Warehouse warehouse(Long id, String code) {
        return warehouse(id, code, null);
    }

    private Warehouse warehouse(Long id, String code, Tenant tenant) {
        return Warehouse.builder()
            .id(id)
            .tenant(tenant)
            .code(code)
            .name(code)
            .location("Test")
            .build();
    }

    private Inventory inventory(Warehouse warehouse) {
        return Inventory.builder().warehouse(warehouse).build();
    }

    private FulfillmentTask task(Long id, Warehouse warehouse) {
        return FulfillmentTask.builder()
            .id(id)
            .warehouse(warehouse)
            .status(FulfillmentStatus.QUEUED)
            .build();
    }
}
