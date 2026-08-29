package com.synapsecore.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.Recommendation;
import com.synapsecore.domain.entity.RecommendationType;
import com.synapsecore.domain.entity.Warehouse;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecommendationScopeServiceTest {

    @Test
    void warehouseOperatorSeesOnlyOwnedRecommendationsAndMustOwnBothTransferWarehouses() {
        AccessOperator operator = AccessOperator.builder().actorName("North operator").warehouseScopes(Set.of("WH-NORTH")).build();
        AccessDirectoryService directory = new AccessDirectoryService(null, null, null, null, null) {
            @Override
            public Optional<AccessOperator> getCurrentOperator() {
                return Optional.of(operator);
            }
        };
        RecommendationScopeService scopeService = new RecommendationScopeService(directory);
        Warehouse north = Warehouse.builder().code("WH-NORTH").name("North").location("North").build();
        Warehouse coast = Warehouse.builder().code("WH-COAST").name("Coast").location("Coast").build();

        Recommendation northRecommendation = Recommendation.builder().warehouse(north).type(RecommendationType.REORDER_STOCK).build();
        Recommendation coastRecommendation = Recommendation.builder().warehouse(coast).type(RecommendationType.REORDER_STOCK).build();
        Recommendation crossWarehouseTransfer = Recommendation.builder()
            .warehouse(north)
            .sourceWarehouse(coast)
            .destinationWarehouse(north)
            .type(RecommendationType.TRANSFER_STOCK)
            .build();

        assertThat(scopeService.visible(List.of(northRecommendation, coastRecommendation, crossWarehouseTransfer)))
            .containsExactly(northRecommendation);
    }

    @Test
    void tenantWideOperatorSeesBothWarehouseRecommendations() {
        AccessOperator operator = AccessOperator.builder().actorName("Tenant operator").warehouseScopes(Set.of()).build();
        AccessDirectoryService directory = new AccessDirectoryService(null, null, null, null, null) {
            @Override
            public Optional<AccessOperator> getCurrentOperator() {
                return Optional.of(operator);
            }
        };
        RecommendationScopeService scopeService = new RecommendationScopeService(directory);
        Warehouse north = Warehouse.builder().code("WH-NORTH").name("North").location("North").build();
        Warehouse coast = Warehouse.builder().code("WH-COAST").name("Coast").location("Coast").build();
        Recommendation northRecommendation = Recommendation.builder().warehouse(north).type(RecommendationType.REORDER_STOCK).build();
        Recommendation coastRecommendation = Recommendation.builder().warehouse(coast).type(RecommendationType.REORDER_STOCK).build();

        assertThat(scopeService.visible(List.of(northRecommendation, coastRecommendation)))
            .containsExactly(northRecommendation, coastRecommendation);
    }
}
