package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.decision.RecommendationReconciliationEvidenceService;
import com.synapsecore.decision.RecommendationReconciliationService;
import com.synapsecore.domain.entity.AuditLog;
import com.synapsecore.domain.entity.Product;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@ActiveProfiles("test")
@Transactional
class RecommendationReconciliationObservabilityIntegrationTest {

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
    void enabledScheduledInvocationCreatesExactlyOneStartAndCompletionPair() {
        String tenantCode = "RECON-OBS-A-" + UUID.randomUUID();
        inventory(tenantCode, "SKU-OBS-A");

        reconciliationService.reconcileOnSchedule();

        List<AuditLog> evidence = evidenceFor(tenantCode);
        assertThat(evidence).hasSize(2);
        AuditLog started = findAction(evidence, "RECOMMENDATION_RECONCILIATION_STARTED");
        AuditLog completed = findAction(evidence, "RECOMMENDATION_RECONCILIATION_COMPLETED");
        assertThat(started.getTargetRef()).isNotBlank();
        assertThat(completed.getTargetRef()).isEqualTo(started.getTargetRef());
        assertThat(started.getDetails()).contains("trigger=SCHEDULED", "status=STARTED");
        assertThat(completed.getDetails()).contains("trigger=SCHEDULED", "status=COMPLETED");
    }

    @Test
    void scheduledEvidenceIdentifiesTheClockTriggeredEntryPoint() {
        String tenantCode = "RECON-OBS-B-" + UUID.randomUUID();
        inventory(tenantCode, "SKU-OBS-B");

        reconciliationService.reconcileOnSchedule();

        AuditLog completed = findAction(evidenceFor(tenantCode), "RECOMMENDATION_RECONCILIATION_COMPLETED");
        assertThat(completed.getDetails()).contains("runId=" + completed.getTargetRef(), "trigger=SCHEDULED");
        assertThat(completed.getRequestId()).startsWith("rec-scheduler-");
    }

    @Test
    void schedulerCompletionRecordsSuccessfulOutcomeAndTimes() {
        String tenantCode = "RECON-OBS-C-" + UUID.randomUUID();
        inventory(tenantCode, "SKU-OBS-C");

        reconciliationService.reconcileOnSchedule();

        AuditLog completed = findAction(evidenceFor(tenantCode), "RECOMMENDATION_RECONCILIATION_COMPLETED");
        assertThat(completed.getStatus()).isEqualTo(com.synapsecore.domain.entity.AuditStatus.SUCCESS);
        assertThat(completed.getDetails()).contains(
            "outcome=COMPLETED",
            "startedAt=",
            "completedAt=",
            "durationMs="
        );
    }

    @Test
    void schedulerCompletionReportsAttemptedSuccessfulAndFailedCounters() {
        String tenantCode = "RECON-OBS-D-" + UUID.randomUUID();
        inventory(tenantCode, "SKU-OBS-D");

        reconciliationService.reconcileOnSchedule();

        AuditLog completed = findAction(evidenceFor(tenantCode), "RECOMMENDATION_RECONCILIATION_COMPLETED");
        assertThat(completed.getDetails()).contains(
            "inventory=attempted:1,succeeded:1,failed:0",
            "fulfillment=attempted:0,succeeded:0,failed:0",
            "retirements=attempted:0,succeeded:0,failed:0"
        );
    }

    @Test
    void directReconcileNowCannotMasqueradeAsScheduledRun() {
        String tenantCode = "RECON-OBS-F-" + UUID.randomUUID();
        inventory(tenantCode, "SKU-OBS-F");

        reconciliationService.reconcileNow();

        assertThat(evidenceFor(tenantCode)).isEmpty();
    }

    @Test
    void disabledSchedulerCreatesNoFalseRun() {
        String tenantCode = "RECON-OBS-G-" + UUID.randomUUID();
        inventory(tenantCode, "SKU-OBS-G");
        boolean enabled = (boolean) ReflectionTestUtils.getField(reconciliationService, "enabled");
        ReflectionTestUtils.setField(reconciliationService, "enabled", false);

        try {
            reconciliationService.reconcileOnSchedule();
        } finally {
            ReflectionTestUtils.setField(reconciliationService, "enabled", enabled);
        }

        assertThat(evidenceFor(tenantCode)).isEmpty();
        assertThat(ReflectionTestUtils.getField(reconciliationService, "requestTraceContext"))
            .isInstanceOf(RequestTraceContext.class);
    }

    private AuditLog findAction(List<AuditLog> evidence, String action) {
        return evidence.stream().filter(log -> action.equals(log.getAction())).findFirst().orElseThrow();
    }

    private List<AuditLog> evidenceFor(String tenantCode) {
        return auditLogRepository.findAll().stream()
            .filter(log -> tenantCode.equalsIgnoreCase(log.getTenantCode()))
            .filter(log -> "RecommendationSchedulerRun".equals(log.getTargetType()))
            .toList();
    }

    private void inventory(String tenantCode, String sku) {
        Tenant tenant = tenantRepository.save(Tenant.builder().code(tenantCode).name("Reconciliation observability").build());
        Warehouse warehouse = warehouseRepository.save(Warehouse.builder()
            .tenant(tenant)
            .code("WH-OBS")
            .name("Observability warehouse")
            .location("Test")
            .build());
        Product product = productRepository.save(Product.builder()
            .tenant(tenant)
            .catalogSku(sku)
            .name("Observability item")
            .category("Test")
            .build());
        inventoryRepository.save(com.synapsecore.domain.entity.Inventory.builder()
            .warehouse(warehouse)
            .product(product)
            .quantityOnHand(20L)
            .quantityReserved(0L)
            .quantityAvailable(20L)
            .reorderThreshold(10L)
            .build());
    }
}
