package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.decision.RecommendationReconciliationEvidenceService;
import com.synapsecore.decision.RecommendationReconciliationService;
import com.synapsecore.domain.entity.AuditLog;
import com.synapsecore.domain.entity.Inventory;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.intelligence.InventoryMonitoringService;
import java.util.List;
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
@Import(RecommendationReconciliationFailureAccountingIntegrationTest.FailureTestConfiguration.class)
@Transactional
class RecommendationReconciliationFailureAccountingIntegrationTest {

    @Autowired
    private RecommendationReconciliationService reconciliationService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void onePerItemExceptionProducesCompletedWithFailuresAndAccurateCounters() {
        String tenantCode = "RECON-OBS-E-" + UUID.randomUUID();
        inventory(tenantCode, "SKU-OBS-E-SUCCESS");
        inventory(tenantCode, "SKU-OBS-E-FAIL");

        reconciliationService.reconcileOnSchedule();

        AuditLog completed = auditLogRepository.findAll().stream()
            .filter(log -> tenantCode.equalsIgnoreCase(log.getTenantCode()))
            .filter(log -> "RECOMMENDATION_RECONCILIATION_COMPLETED".equals(log.getAction()))
            .findFirst()
            .orElseThrow();
        assertThat(completed.getDetails()).contains(
            "outcome=COMPLETED_WITH_FAILURES",
            "inventory=attempted:2,succeeded:1,failed:1"
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailureTestConfiguration {

        @Bean
        @Primary
        InventoryMonitoringService failingInventoryMonitoringService() {
            return new InventoryMonitoringService(null, null, null, null) {
                @Override
                public void evaluateAfterChange(Inventory inventory, String source) {
                    if (inventory.getProduct().resolveCatalogSku().endsWith("FAIL")) {
                        throw new IllegalStateException("synthetic per-item failure");
                    }
                }
            };
        }
    }

    private void inventory(String tenantCode, String sku) {
        Tenant tenant = tenantRepository.findByCodeIgnoreCase(tenantCode)
            .orElseGet(() -> tenantRepository.save(Tenant.builder()
                .code(tenantCode)
                .name("Reconciliation failure accounting")
                .build()));
        Warehouse warehouse = warehouseRepository.findByTenant_CodeIgnoreCaseAndCode(tenantCode, "WH-OBS")
            .orElseGet(() -> warehouseRepository.save(Warehouse.builder()
                .tenant(tenant)
                .code("WH-OBS")
                .name("Observability warehouse")
                .location("Test")
                .build()));
        Product product = productRepository.save(Product.builder()
            .tenant(tenant)
            .catalogSku(sku)
            .name(sku)
            .category("Test")
            .build());
        inventoryRepository.save(Inventory.builder()
            .warehouse(warehouse)
            .product(product)
            .quantityOnHand(20L)
            .quantityReserved(0L)
            .quantityAvailable(20L)
            .reorderThreshold(10L)
            .build());
    }
}
