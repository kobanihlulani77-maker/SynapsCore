package com.synapsecore.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.alert.AlertScopeService;
import com.synapsecore.decision.RecommendationScopeService;
import com.synapsecore.domain.dto.FulfillmentOverviewResponse;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.tenant.TenantContextService;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DashboardServiceSnapshotReuseTest {

    @Test
    void summaryUsesSuppliedFulfillmentSnapshotWithoutRecomposingIt() {
        FulfillmentOverviewResponse fulfillment = new FulfillmentOverviewResponse(
            7, 3, 2, 1, List.of(), Instant.now()
        );
        DashboardService service = new DashboardService(
            zeroRepository(CustomerOrderRepository.class),
            zeroRepository(InventoryRepository.class),
            zeroRepository(RecommendationRepository.class),
            zeroRepository(WarehouseRepository.class),
            null,
            null,
            new ObjectMapper(),
            tenantContextService(),
            alertScopeService(),
            accessDirectoryService()
        );
        ReflectionTestUtils.setField(service, "recommendationScopeService", new RecommendationScopeService(null) {
            @Override
            public List<com.synapsecore.domain.entity.Recommendation> visible(
                    List<com.synapsecore.domain.entity.Recommendation> recommendations) {
                return recommendations;
            }
        });
        ReflectionTestUtils.setField(service, "cacheEnabled", false);

        var summary = service.getSummary(fulfillment);

        assertThat(summary.fulfillmentBacklogCount()).isEqualTo(7);
        assertThat(summary.delayedShipmentCount()).isEqualTo(2);
        assertThat(summary.fulfillmentRiskCount()).isEqualTo(1);
    }

    @Test
    void summaryUsesSuppliedRecommendationCountWithoutReloadingRecommendations() {
        FulfillmentOverviewResponse fulfillment = new FulfillmentOverviewResponse(
            0, 0, 0, 0, List.of(), Instant.now()
        );
        RecommendationRepository recommendations = (RecommendationRepository) Proxy.newProxyInstance(
            RecommendationRepository.class.getClassLoader(),
            new Class<?>[]{RecommendationRepository.class},
            (proxy, method, args) -> {
                throw new AssertionError("Unexpected recommendation repository call: " + method.getName());
            }
        );
        DashboardService service = new DashboardService(
            zeroRepository(CustomerOrderRepository.class),
            zeroRepository(InventoryRepository.class),
            recommendations,
            zeroRepository(WarehouseRepository.class),
            null,
            null,
            new ObjectMapper(),
            tenantContextService(),
            alertScopeService(),
            accessDirectoryService()
        );
        ReflectionTestUtils.setField(service, "cacheEnabled", false);

        var summary = service.getSummary(fulfillment, 11L);

        assertThat(summary.recommendationsCount()).isEqualTo(11);
    }

    @Test
    void summaryUsesSuppliedAlertCountWithoutReloadingAlerts() {
        FulfillmentOverviewResponse fulfillment = new FulfillmentOverviewResponse(
            0, 0, 0, 0, List.of(), Instant.now()
        );
        AlertScopeService alerts = new AlertScopeService(null, null) {
            @Override
            public boolean isCurrentOperatorWarehouseScoped() {
                return false;
            }

            @Override
            public long countVisibleActiveAlerts(String tenantCode) {
                throw new AssertionError("Active alerts must not be reloaded when a snapshot count is supplied");
            }
        };
        DashboardService service = new DashboardService(
            zeroRepository(CustomerOrderRepository.class),
            zeroRepository(InventoryRepository.class),
            zeroRepository(RecommendationRepository.class),
            zeroRepository(WarehouseRepository.class),
            null,
            null,
            new ObjectMapper(),
            tenantContextService(),
            alerts,
            accessDirectoryService()
        );
        ReflectionTestUtils.setField(service, "cacheEnabled", false);

        var summary = service.getSummary(fulfillment, 0L, 13L);

        assertThat(summary.activeAlerts()).isEqualTo(13);
    }

    private TenantContextService tenantContextService() {
        return new TenantContextService(null, null, null, null, null) {
            @Override
            public String getCurrentTenantCodeOrDefault() {
                return "SNAPSHOT-REUSE";
            }
        };
    }

    private AlertScopeService alertScopeService() {
        return new AlertScopeService(null, null) {
            @Override
            public boolean isCurrentOperatorWarehouseScoped() {
                return false;
            }

            @Override
            public long countVisibleActiveAlerts(String tenantCode) {
                return 0;
            }
        };
    }

    private AccessDirectoryService accessDirectoryService() {
        return new AccessDirectoryService(null, null, null, null, null) {
            @Override
            public Optional<com.synapsecore.domain.entity.AccessOperator> getCurrentOperator() {
                return Optional.empty();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <T> T zeroRepository(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getReturnType() == long.class) {
                return 0L;
            }
            if (List.class.isAssignableFrom(method.getReturnType())) {
                return List.of();
            }
            throw new AssertionError("Unexpected repository call: " + method.getName());
        });
    }
}
