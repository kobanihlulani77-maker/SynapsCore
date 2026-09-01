package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.synapsecore.decision.RecommendationReconciliationEvidenceService;
import com.synapsecore.decision.RecommendationReconciliationService;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@ActiveProfiles("test")
@Import(RecommendationReconciliationEvidenceFailureIntegrationTest.EvidenceFailureTestConfiguration.class)
@Transactional
class RecommendationReconciliationEvidenceFailureIntegrationTest {

    @Autowired
    private RecommendationReconciliationService reconciliationService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void evidenceFinalizationFailureDoesNotCorruptInventoryOrRecommendationTruth() {
        String tenantCode = "RECON-OBS-H-" + UUID.randomUUID();
        Tenant tenant = tenantRepository.save(Tenant.builder()
            .code(tenantCode)
            .name("Reconciliation evidence failure")
            .build());
        Warehouse warehouse = warehouseRepository.save(Warehouse.builder()
            .tenant(tenant)
            .code("WH-OBS")
            .name("Observability warehouse")
            .location("Test")
            .build());
        Product product = productRepository.save(Product.builder()
            .tenant(tenant)
            .catalogSku("SKU-OBS-H")
            .name("Stable observability item")
            .category("Test")
            .build());
        Inventory inventory = inventoryRepository.save(Inventory.builder()
            .warehouse(warehouse)
            .product(product)
            .quantityOnHand(20L)
            .quantityReserved(0L)
            .quantityAvailable(20L)
            .reorderThreshold(10L)
            .build());
        long recommendationsBefore = recommendationRepository.count();

        assertThatCode(() -> reconciliationService.reconcileOnSchedule())
            .doesNotThrowAnyException();

        Inventory readback = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(readback.getQuantityOnHand()).isEqualTo(20L);
        assertThat(readback.getQuantityAvailable()).isEqualTo(20L);
        assertThat(recommendationRepository.count()).isEqualTo(recommendationsBefore);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EvidenceFailureTestConfiguration {

        @Bean
        @Primary
        RecommendationReconciliationEvidenceService failingEvidenceService() {
            return new RecommendationReconciliationEvidenceService(null) {
                @Override
                public void recordStarted(String runId, Instant startedAt, Collection<String> tenantCodes) {
                    // The scheduler must continue if the start evidence sink is unavailable.
                }

                @Override
                public void recordCompleted(String runId,
                                             Instant startedAt,
                                             Instant completedAt,
                                             String outcome,
                                             Map<String, ReconciliationTenantCounts> countsByTenant) {
                    throw new IllegalStateException("synthetic evidence sink failure");
                }
            };
        }
    }
}
