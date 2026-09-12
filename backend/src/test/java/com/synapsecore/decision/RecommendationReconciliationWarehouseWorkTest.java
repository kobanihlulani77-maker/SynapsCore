package com.synapsecore.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.FulfillmentTask;
import com.synapsecore.domain.entity.Warehouse;
import java.util.List;
import java.util.Map;
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

    private Warehouse warehouse(Long id, String code) {
        return Warehouse.builder()
            .id(id)
            .code(code)
            .name(code)
            .location("Test")
            .build();
    }

    private FulfillmentTask task(Long id, Warehouse warehouse) {
        return FulfillmentTask.builder()
            .id(id)
            .warehouse(warehouse)
            .status(FulfillmentStatus.QUEUED)
            .build();
    }
}
