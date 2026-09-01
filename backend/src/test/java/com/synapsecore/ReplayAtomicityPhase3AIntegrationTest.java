package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.integration.IntegrationFailureCode;
import com.synapsecore.domain.entity.IntegrationInboundStatus;
import com.synapsecore.domain.entity.IntegrationReplayRecord;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.IntegrationInboundRecordRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.integration.IntegrationReplayService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "synapsecore.starter.require-explicit-tenant-provisioning=true",
    "synapsecore.starter.seed-starter-inventory-on-tenant-onboarding=false",
    "synapsecore.starter.seed-starter-connectors-on-tenant-onboarding=false",
    "synapsecore.dashboard.cache-enabled=false",
    "synapsecore.integration.pull-worker.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(ReplayAtomicityPhase3AIntegrationTest.ReplayAtomicityTestConfiguration.class)
class ReplayAtomicityPhase3AIntegrationTest {

    private static final String PLATFORM_ADMIN_TOKEN = "test-only-platform-admin-token";
    private static final String ADMIN_PASSWORD = "Phase3AAdmin!2026";
    private static final String INITIAL_PASSWORD = "Phase3AInitial!2026";
    private static final String ROLE_PASSWORD = "Phase3ARole!2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private IntegrationInboundRecordRepository inboundRecordRepository;

    @Autowired
    private IntegrationReplayRecordRepository replayRecordRepository;

    @Autowired
    private BusinessEventRepository businessEventRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private IntegrationReplayService integrationReplayService;

    @Autowired
    private RequestTraceContext requestTraceContext;

    @Autowired
    private ReplayFatalFailureProbe replayFatalFailureProbe;

    private String suffix;
    private String tenantA;
    private String tenantB;
    private String warehouseA;
    private String warehouseB;
    private String sourceA;
    private String sourceB;
    private String validSkuA;
    private String validSkuB;
    private MockHttpSession tenantAdminA;
    private MockHttpSession integrationAdminA;
    private MockHttpSession integrationOperatorA;
    private MockHttpSession tenantAdminB;
    private MockHttpSession integrationAdminB;
    private MockHttpSession integrationOperatorB;

    @BeforeEach
    void provisionReplayAtomicityFixtures() throws Exception {
        suffix = Long.toString(System.nanoTime());
        tenantA = "P3A-A-" + suffix;
        tenantB = "P3A-B-" + suffix;
        warehouseA = "P3A-WHA-" + suffix;
        warehouseB = "P3A-WHB-" + suffix;
        sourceA = "p3a-csv-a-" + suffix;
        sourceB = "p3a-csv-b-" + suffix;
        validSkuA = "P3A-SKU-A-" + suffix;
        validSkuB = "P3A-SKU-B-" + suffix;

        provisionTenant(tenantPayload(tenantA, "Tenant A", warehouseA, "p3a.admin.a." + suffix,
            "p3a.integration.admin.a." + suffix, "p3a.integration.operator.a." + suffix));
        provisionTenant(tenantPayload(tenantB, "Tenant B", warehouseB, "p3a.admin.b." + suffix,
            "p3a.integration.admin.b." + suffix, "p3a.integration.operator.b." + suffix));

        tenantAdminA = login(tenantA, "p3a.admin.a." + suffix, ADMIN_PASSWORD, "TENANT_ADMIN", List.of());
        integrationAdminA = login(tenantA, "p3a.integration.admin.a." + suffix, INITIAL_PASSWORD,
            "INTEGRATION_ADMIN", List.of(warehouseA));
        integrationOperatorA = login(tenantA, "p3a.integration.operator.a." + suffix, INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(warehouseA));
        tenantAdminB = login(tenantB, "p3a.admin.b." + suffix, ADMIN_PASSWORD, "TENANT_ADMIN", List.of());
        integrationAdminB = login(tenantB, "p3a.integration.admin.b." + suffix, INITIAL_PASSWORD,
            "INTEGRATION_ADMIN", List.of(warehouseB));
        integrationOperatorB = login(tenantB, "p3a.integration.operator.b." + suffix, INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(warehouseB));

        createProduct(tenantAdminA, validSkuA);
        createProduct(tenantAdminB, validSkuB);
        createConnector(integrationAdminA, sourceA, warehouseA);
        createConnector(integrationAdminB, sourceB, warehouseB);
    }

    @Test
    void automatedBatchTypedFailureRollsBackEarlierSuccessAndEvidenceTogether() throws Exception {
        String successfulOrder = "P3A-A-" + suffix;
        String failedOrder = "P3A-B-" + suffix;
        String lowStockSku = "P3A-LOW-" + suffix;
        createProduct(tenantAdminA, lowStockSku);
        createInventory(tenantAdminA, lowStockSku, warehouseA, 1);

        importCsv(integrationOperatorA, sourceA, warehouseA, successfulOrder, validSkuA)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("INVENTORY_NOT_FOUND"));
        importCsv(integrationOperatorA, sourceA, warehouseA, failedOrder, lowStockSku)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("INSUFFICIENT_INVENTORY"));

        IntegrationReplayRecord successfulReplay = replayRecord(tenantA, successfulOrder);
        IntegrationReplayRecord failedReplay = replayRecord(tenantA, failedOrder);
        var successfulInbound = inboundRecord(tenantA, successfulOrder);
        var failedInbound = inboundRecord(tenantA, failedOrder);
        createInventory(tenantAdminA, validSkuA, warehouseA, 8);

        assertThat(integrationReplayService.processAutomatedReplayBatch(2)).isEqualTo(1);

        IntegrationReplayRecord completedReplay = replayRecordRepository.findById(successfulReplay.getId()).orElseThrow();
        assertThat(completedReplay.getStatus()).isEqualTo(IntegrationReplayStatus.REPLAYED);
        assertThat(completedReplay.getReplayAttemptCount()).isEqualTo(1);
        assertThat(completedReplay.getReplayedOrderExternalId()).isEqualTo(successfulOrder);
        assertThat(completedReplay.getResolvedAt()).isNotNull();

        IntegrationReplayRecord failedReplayAfterAttempt = replayRecordRepository.findById(failedReplay.getId()).orElseThrow();
        assertThat(failedReplayAfterAttempt.getStatus()).isEqualTo(IntegrationReplayStatus.REPLAY_FAILED);
        assertThat(failedReplayAfterAttempt.getReplayAttemptCount()).isEqualTo(1);
        assertThat(failedReplayAfterAttempt.getFailureCode()).isEqualTo(IntegrationFailureCode.INSUFFICIENT_INVENTORY);
        assertThat(failedReplayAfterAttempt.getNextEligibleAt()).isNotNull();
        assertThat(failedReplayAfterAttempt.getResolvedAt()).isNull();

        assertThat(inboundRecordRepository.findById(successfulInbound.getId()).orElseThrow().getStatus())
            .isEqualTo(IntegrationInboundStatus.REPLAYED);
        assertThat(inboundRecordRepository.findById(failedInbound.getId()).orElseThrow().getStatus())
            .isEqualTo(IntegrationInboundStatus.REPLAY_QUEUED);
        assertThat(customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(tenantA, successfulOrder))
            .isPresent();
        assertThat(customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(tenantA, failedOrder))
            .isEmpty();
        assertInventory(tenantA, validSkuA, warehouseA, 2L, 6L);
        assertInventory(tenantA, lowStockSku, warehouseA, 0L, 1L);
        assertThat(businessEventRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(tenantA))
            .anyMatch(event -> event.getEventType() == BusinessEventType.INTEGRATION_REPLAY_COMPLETED
                && event.getPayloadSummary().contains(successfulOrder));
        assertThat(auditLogRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(tenantA))
            .anyMatch(audit -> audit.getAction().equals("INTEGRATION_REPLAY_COMPLETED")
                && audit.getStatus() == AuditStatus.SUCCESS
                && audit.getDetails().contains(successfulOrder));
        assertTraceContextClear();
    }

    @Test
    void fatalFailureRollsBackEarlierReplayInTheSameTenantBatch() throws Exception {
        String successfulOrder = "P3A-C-" + suffix;
        String fatalOrder = "P3A-D-" + suffix;
        String missingSku = "P3A-MISSING-" + suffix;
        createProduct(tenantAdminA, missingSku);

        importCsv(integrationOperatorA, sourceA, warehouseA, successfulOrder, missingSku)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("INVENTORY_NOT_FOUND"));
        importCsv(integrationOperatorA, sourceA, warehouseA, fatalOrder, missingSku)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("INVENTORY_NOT_FOUND"));

        IntegrationReplayRecord successfulReplay = replayRecord(tenantA, successfulOrder);
        IntegrationReplayRecord fatalReplay = replayRecord(tenantA, fatalOrder);
        createInventory(tenantAdminA, missingSku, warehouseA, 8);

        replayFatalFailureProbe.reset();
        replayFatalFailureProbe.armFor(fatalOrder);
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(
            () -> integrationReplayService.processAutomatedReplayBatch(2));
        replayFatalFailureProbe.clear();
        requestTraceContext.clear();

        assertThat(thrown)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Phase 3A fatal replay test failure for " + fatalOrder);
        assertThat(replayFatalFailureProbe.wasFatalThrown()).isTrue();
        assertThat(replayFatalFailureProbe.seenOrderIds()).containsExactly(successfulOrder, fatalOrder);

        assertCommittedReplay(successfulReplay, successfulOrder);
        assertRolledBackReplay(fatalReplay, fatalOrder);
        assertThat(customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(tenantA, successfulOrder))
            .hasValueSatisfying(order -> assertThat(order.getExternalOrderId()).isEqualTo(successfulOrder));
        assertThat(customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(tenantA, fatalOrder))
            .isEmpty();
        assertInventory(tenantA, missingSku, warehouseA, 2L, 6L);
        assertTraceContextClear();
    }

    @Test
    void fatalFailureRollsBackEarlierReplayAcrossTenantBoundaries() throws Exception {
        String tenantAOrder = "P3A-E-" + suffix;
        String tenantBOrder = "P3A-F-" + suffix;
        String missingSkuA = "P3A-MISSING-A-" + suffix;
        String missingSkuB = "P3A-MISSING-B-" + suffix;
        createProduct(tenantAdminA, missingSkuA);
        createProduct(tenantAdminB, missingSkuB);

        importCsv(integrationOperatorA, sourceA, warehouseA, tenantAOrder, missingSkuA)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("INVENTORY_NOT_FOUND"));
        importCsv(integrationOperatorB, sourceB, warehouseB, tenantBOrder, missingSkuB)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("INVENTORY_NOT_FOUND"));

        IntegrationReplayRecord tenantAReplay = replayRecord(tenantA, tenantAOrder);
        IntegrationReplayRecord tenantBReplay = replayRecord(tenantB, tenantBOrder);
        createInventory(tenantAdminA, missingSkuA, warehouseA, 8);
        createInventory(tenantAdminB, missingSkuB, warehouseB, 8);

        replayFatalFailureProbe.reset();
        replayFatalFailureProbe.armFor(tenantBOrder);
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(
            () -> integrationReplayService.processAutomatedReplayBatch(2));
        replayFatalFailureProbe.clear();
        requestTraceContext.clear();

        assertThat(thrown)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Phase 3A fatal replay test failure for " + tenantBOrder);
        assertThat(replayFatalFailureProbe.wasFatalThrown()).isTrue();
        assertThat(replayFatalFailureProbe.seenOrderIds()).containsExactly(tenantAOrder, tenantBOrder);

        assertCommittedReplay(tenantAReplay, tenantAOrder);
        assertRolledBackReplay(tenantBReplay, tenantBOrder);
        assertThat(customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(tenantA, tenantAOrder))
            .isPresent();
        assertThat(customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(tenantB, tenantBOrder))
            .isEmpty();
        assertInventory(tenantA, missingSkuA, warehouseA, 2L, 6L);
        assertInventory(tenantB, missingSkuB, warehouseB, 0L, 8L);
        assertTraceContextClear();
    }

    @Test
    void manualInsufficientInventoryFailureCommitsDurableFailureEvidence() throws Exception {
        String externalOrderId = "P3A-MANUAL-" + suffix;
        String sku = "P3A-MANUAL-SKU-" + suffix;
        createProduct(tenantAdminA, sku);
        createInventory(tenantAdminA, sku, warehouseA, 1);

        importCsv(integrationOperatorA, sourceA, warehouseA, externalOrderId, sku)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("INSUFFICIENT_INVENTORY"));

        IntegrationReplayRecord replay = replayRecord(tenantA, externalOrderId);
        mockMvc.perform(post("/api/integrations/orders/replay/" + replay.getId())
                .session(integrationOperatorA)
                .header("X-Synapse-Tenant", tenantA))
            .andExpect(status().isConflict());

        IntegrationReplayRecord failedReplay = replayRecordRepository.findById(replay.getId()).orElseThrow();
        assertThat(failedReplay.getStatus()).isEqualTo(IntegrationReplayStatus.REPLAY_FAILED);
        assertThat(failedReplay.getReplayAttemptCount()).isEqualTo(1);
        assertThat(failedReplay.getFailureCode()).isEqualTo(IntegrationFailureCode.INSUFFICIENT_INVENTORY);
        assertThat(failedReplay.getNextEligibleAt()).isNotNull();
        assertThat(customerOrderRepository.findByTenant_CodeIgnoreCaseAndExternalOrderId(tenantA, externalOrderId))
            .isEmpty();
        assertInventory(tenantA, sku, warehouseA, 0L, 1L);
        assertThat(businessEventRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(tenantA))
            .anyMatch(event -> event.getEventType() == BusinessEventType.INTEGRATION_REPLAY_FAILED
                && event.getPayloadSummary().contains(externalOrderId));
        assertThat(auditLogRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(tenantA))
            .anyMatch(audit -> audit.getAction().equals("INTEGRATION_REPLAY_FAILED")
                && audit.getStatus() == AuditStatus.FAILURE
                && audit.getDetails().contains(externalOrderId));
    }

    private void assertCommittedReplay(IntegrationReplayRecord original, String externalOrderId) {
        IntegrationReplayRecord fresh = replayRecordRepository.findById(original.getId()).orElseThrow();
        assertThat(fresh.getStatus()).as(externalOrderId).isEqualTo(IntegrationReplayStatus.REPLAYED);
        assertThat(fresh.getReplayAttemptCount()).as(externalOrderId).isEqualTo(1);
        assertThat(fresh.getReplayedOrderExternalId()).as(externalOrderId).isEqualTo(externalOrderId);
        assertThat(fresh.getResolvedAt()).as(externalOrderId).isNotNull();
    }

    private void assertRolledBackReplay(IntegrationReplayRecord original, String externalOrderId) {
        IntegrationReplayRecord fresh = replayRecordRepository.findById(original.getId()).orElseThrow();
        assertThat(fresh.getStatus()).as(externalOrderId).isEqualTo(IntegrationReplayStatus.PENDING);
        assertThat(fresh.getReplayAttemptCount()).as(externalOrderId).isZero();
        assertThat(fresh.getReplayedOrderExternalId()).as(externalOrderId).isNull();
        assertThat(fresh.getResolvedAt()).as(externalOrderId).isNull();
    }

    private void assertInventory(String tenantCode, String sku, String warehouseCode,
                                 long reserved, long available) {
        var product = productRepository.findByTenant_CodeIgnoreCaseAndCatalogSkuIgnoreCase(tenantCode, sku)
            .orElseThrow();
        var warehouse = warehouseRepository.findByCode(warehouseCode).orElseThrow();
        assertThat(inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()))
            .get()
            .extracting(item -> item.getQuantityReserved(), item -> item.getQuantityAvailable())
            .containsExactly(reserved, available);
    }

    private void assertTraceContextClear() {
        assertThat(requestTraceContext.getCurrentActor()).isEmpty();
        assertThat(requestTraceContext.getCurrentTenant()).isEmpty();
        assertThat(requestTraceContext.getCurrentRequestId()).isEmpty();
    }

    private org.springframework.test.web.servlet.ResultActions importCsv(MockHttpSession session, String source,
                                                                          String warehouse, String externalOrderId,
                                                                          String sku) throws Exception {
        String csv = "sourceSystem,externalOrderId,warehouseCode,productSku,quantity,unitPrice\n"
            + "%s,%s,%s,%s,2,20.00\n".formatted(source, externalOrderId, warehouse, sku);
        MockMultipartFile file = new MockMultipartFile(
            "file", externalOrderId + ".csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        return mockMvc.perform(multipart("/api/integrations/orders/csv-import")
            .file(file).session(session).param("sourceSystem", source));
    }

    private IntegrationReplayRecord replayRecord(String tenantCode, String externalOrderId) {
        return replayRecordRepository.findAll().stream()
            .filter(record -> tenantCode.equalsIgnoreCase(record.getTenantCode()))
            .filter(record -> externalOrderId.equals(record.getExternalOrderId()))
            .findFirst().orElseThrow();
    }

    private com.synapsecore.domain.entity.IntegrationInboundRecord inboundRecord(
        String tenantCode, String externalOrderId) {
        return inboundRecordRepository.findAll().stream()
            .filter(record -> tenantCode.equalsIgnoreCase(record.getTenantCode()))
            .filter(record -> externalOrderId.equals(record.getExternalOrderId()))
            .findFirst().orElseThrow();
    }

    private void provisionTenant(String payload) throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON).content(payload))
            .andExpect(status().isOk());
    }

    private MockHttpSession login(String tenantCode, String username, String password, String expectedRole,
                                  List<String> scopes) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON).content(loginPayload(tenantCode, username, password)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.hasItem(expectedRole))).andReturn();
        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        if (body.path("passwordChangeRequired").asBoolean()) {
            MvcResult changed = mockMvc.perform(post("/api/auth/session/password").session(session)
                    .contentType(APPLICATION_JSON)
                    .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}"
                        .formatted(password, ROLE_PASSWORD)))
                .andExpect(status().isOk()).andReturn();
            session = (MockHttpSession) changed.getRequest().getSession(false);
        }
        JsonNode sessionBody = objectMapper.readTree(mockMvc.perform(get("/api/auth/session").session(session))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(arrayTexts(sessionBody.path("warehouseScopes"))).containsExactlyElementsOf(scopes);
        return session;
    }

    private void createProduct(MockHttpSession session, String sku) throws Exception {
        mockMvc.perform(post("/api/products").session(session).contentType(APPLICATION_JSON)
                .content("{\"sku\":\"%s\",\"name\":\"Phase 3A %s\",\"category\":\"Recovery\"}"
                    .formatted(sku, sku)))
            .andExpect(status().isCreated());
    }

    private void createInventory(MockHttpSession session, String sku, String warehouse, long quantity)
        throws Exception {
        mockMvc.perform(post("/api/inventory/update").session(session).contentType(APPLICATION_JSON)
                .content("{\"productSku\":\"%s\",\"warehouseCode\":\"%s\",\"quantityAvailable\":%d,\"reorderThreshold\":0}"
                    .formatted(sku, warehouse, quantity)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantityAvailable").value(quantity));
    }

    private void createConnector(MockHttpSession session, String source, String warehouse) throws Exception {
        mockMvc.perform(post("/api/integrations/orders/connectors").session(session).contentType(APPLICATION_JSON)
                .content("{\"sourceSystem\":\"%s\",\"type\":\"CSV_ORDER_IMPORT\",\"displayName\":\"Phase 3A %s\",\"enabled\":true,\"syncMode\":\"BATCH_FILE_DROP\",\"validationPolicy\":\"STANDARD\",\"transformationPolicy\":\"NORMALIZE_CODES\",\"allowDefaultWarehouseFallback\":false,\"defaultWarehouseCode\":\"%s\"}"
                    .formatted(source, source, warehouse)))
            .andExpect(status().isOk());
    }

    private String loginPayload(String tenant, String username, String password) {
        return "{\"tenantCode\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"}"
            .formatted(tenant, username, password);
    }

    private List<String> arrayTexts(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }

    private String tenantPayload(String tenantCode, String label, String warehouseCode,
                                 String adminUsername, String integrationAdminUsername,
                                 String integrationOperatorUsername) {
        return """
            {
              "tenantCode":"%s",
              "tenantName":"Phase 3A %s",
              "description":"Disposable replay atomicity proof tenant.",
              "adminFullName":"Phase 3A %s Admin",
              "adminUsername":"%s",
              "adminPassword":"%s",
              "primaryLocation":"Phase 3A %s",
              "warehouses":[{"code":"%s","name":"Phase 3A %s Warehouse","location":"Phase 3A %s"}],
              "users":[
                {"username":"%s","fullName":"Phase 3A %s Admin","operatorActorName":"Phase 3A %s Admin","roles":["TENANT_ADMIN"],"warehouseScopes":[]},
                {"username":"%s","fullName":"Phase 3A %s Integration Admin","operatorActorName":"Phase 3A %s Integration Admin","roles":["INTEGRATION_ADMIN"],"warehouseScopes":["%s"],"initialPassword":"%s"},
                {"username":"%s","fullName":"Phase 3A %s Integration Operator","operatorActorName":"Phase 3A %s Integration Operator","roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["%s"],"initialPassword":"%s"}
              ],
              "requiredRoles":[]
            }
            """.formatted(tenantCode, label, label, adminUsername, ADMIN_PASSWORD, label, warehouseCode, label,
                label, adminUsername, label, label, integrationAdminUsername, label, label, warehouseCode,
                INITIAL_PASSWORD, integrationOperatorUsername, label, label, warehouseCode, INITIAL_PASSWORD);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ReplayAtomicityTestConfiguration {

        @Bean
        ReplayFatalFailureProbe replayFatalFailureProbe() {
            return new ReplayFatalFailureProbe();
        }

        @Bean
        ReplayFatalFailureAspect replayFatalFailureAspect(ReplayFatalFailureProbe probe) {
            return new ReplayFatalFailureAspect(probe);
        }
    }

    static class ReplayFatalFailureProbe {

        private final List<String> seenOrderIds = new CopyOnWriteArrayList<>();
        private volatile String fatalExternalOrderId;
        private volatile boolean fatalThrown;

        void reset() {
            seenOrderIds.clear();
            fatalExternalOrderId = null;
            fatalThrown = false;
        }

        void armFor(String externalOrderId) {
            fatalExternalOrderId = externalOrderId;
        }

        void clear() {
            fatalExternalOrderId = null;
        }

        void observe(String externalOrderId) {
            seenOrderIds.add(externalOrderId);
            if (externalOrderId != null && externalOrderId.equals(fatalExternalOrderId)) {
                fatalThrown = true;
                throw new IllegalStateException("Phase 3A fatal replay test failure for " + externalOrderId);
            }
        }

        boolean wasFatalThrown() {
            return fatalThrown;
        }

        List<String> seenOrderIds() {
            return List.copyOf(seenOrderIds);
        }
    }

    @Aspect
    static class ReplayFatalFailureAspect {

        private final ReplayFatalFailureProbe probe;

        ReplayFatalFailureAspect(ReplayFatalFailureProbe probe) {
            this.probe = probe;
        }

        @Around("execution(* com.synapsecore.domain.service.OrderService.createOrderForTenant(..))")
        Object injectFatalFailure(ProceedingJoinPoint joinPoint) throws Throwable {
            for (Object argument : joinPoint.getArgs()) {
                if (argument instanceof OrderCreateRequest request) {
                    probe.observe(request.externalOrderId());
                }
            }
            return joinPoint.proceed();
        }
    }
}
