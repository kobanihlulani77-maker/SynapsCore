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
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.CustomerOrder;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.IntegrationInboundStatus;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.IntegrationInboundRecordRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.event.OperationalDispatchQueueService;
import com.synapsecore.integration.IntegrationFailureCode;
import com.synapsecore.integration.IntegrationReplayService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class Layer2Phase4ReplayRecoveryIntegrationTest {

    private static final String PLATFORM_ADMIN_TOKEN = "test-only-platform-admin-token";
    private static final String ADMIN_PASSWORD = "Layer2Phase4Admin!2026";
    private static final String INITIAL_PASSWORD = "Layer2Phase4Initial!2026";
    private static final String ROLE_PASSWORD = "Layer2Phase4Role!2026";
    private static final long DISPATCH_TIMEOUT_MILLIS = 5000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private FulfillmentTaskRepository fulfillmentTaskRepository;

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
    private OperationalDispatchWorkItemRepository dispatchWorkItemRepository;

    @Autowired
    private OperationalDispatchQueueService dispatchQueueService;

    @Autowired
    private IntegrationReplayService integrationReplayService;

    @Autowired
    private RequestTraceContext requestTraceContext;

    private String suffix;
    private String tenantA;
    private String tenantB;
    private String warehouseA;
    private String warehouseB;
    private String tenantBWarehouse;
    private String validSku;
    private String inventoryMissingSku;
    private String insufficientSku;
    private MockHttpSession tenantAdminA;
    private MockHttpSession integrationAdminA;
    private MockHttpSession integrationOperatorA;
    private MockHttpSession warehouseOperatorB;
    private MockHttpSession tenantBOperator;

    @BeforeEach
    void provisionDisposableTenantsAndBaseline() throws Exception {
        suffix = Long.toString(System.nanoTime());
        tenantA = "L2P4-A-" + suffix;
        tenantB = "L2P4-B-" + suffix;
        warehouseA = "L2P4-WHA-" + suffix;
        warehouseB = "L2P4-WHB-" + suffix;
        tenantBWarehouse = "L2P4-WHBT-" + suffix;
        validSku = "L2P4-VALID-" + suffix;
        inventoryMissingSku = "L2P4-NOINV-" + suffix;
        insufficientSku = "L2P4-LOW-" + suffix;

        provisionTenant(tenantPayloadA());
        provisionTenant(tenantPayloadB());

        tenantAdminA = login(tenantA, "p4.admin." + suffix, ADMIN_PASSWORD, "TENANT_ADMIN", List.of());
        integrationAdminA = login(tenantA, "p4.integration.admin." + suffix, INITIAL_PASSWORD,
            "INTEGRATION_ADMIN", List.of(warehouseA));
        integrationOperatorA = login(tenantA, "p4.integration.operator." + suffix, INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(warehouseA));
        warehouseOperatorB = login(tenantA, "p4.warehouse.operator." + suffix, INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(warehouseB));
        tenantBOperator = login(tenantB, "p4.tenantb.operator." + suffix, INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(tenantBWarehouse));

        createProduct(tenantAdminA, validSku);
        updateInventory(tenantAdminA, validSku, warehouseA, 100);
        createProduct(tenantAdminA, inventoryMissingSku);
        createProduct(tenantAdminA, insufficientSku);
        updateInventory(tenantAdminA, insufficientSku, warehouseA, 1);
        createConnector(integrationAdminA, "l2p4-csv", true, warehouseA);
        createConnector(integrationAdminA, "l2p4-disabled", false, warehouseA);
    }

    @Test
    void missingProductRepairReplaysIntoExactlyOneOrderReservationAndQueuedFulfillment() throws Exception {
        String externalOrderId = "L2P4-MISSING-PRODUCT-" + suffix;
        long ordersBefore = customerOrderRepository.countByTenant_CodeIgnoreCase(tenantA);
        String requestId = "l2p4-missing-product-" + suffix;

        importCsv(integrationOperatorA, "l2p4-csv", externalOrderId, validSku + "-MISSING", 3, 18.00)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ordersImported").value(0))
            .andExpect(jsonPath("$.ordersFailed").value(1))
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("PRODUCT_NOT_FOUND"));

        var replay = replayRecord(externalOrderId);
        var inbound = inboundRecord(externalOrderId);
        assertThat(replay.getStatus()).isEqualTo(IntegrationReplayStatus.PENDING);
        assertThat(replay.getFailureCode()).isEqualTo(IntegrationFailureCode.PRODUCT_NOT_FOUND);
        assertThat(replay.getInboundRecordId()).isEqualTo(inbound.getId());
        assertThat(inbound.getStatus()).isEqualTo(IntegrationInboundStatus.REPLAY_QUEUED);
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(tenantA)).isEqualTo(ordersBefore);
        assertThat(replay.getRequestPayload()).doesNotContain("password");

        createProduct(tenantAdminA, validSku + "-MISSING");
        updateInventory(tenantAdminA, validSku + "-MISSING", warehouseA, 10);

        mockMvc.perform(post("/api/integrations/orders/replay/" + replay.getId())
                .session(integrationOperatorA)
                .header("X-Request-Id", requestId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.replay.status").value("REPLAYED"))
            .andExpect(jsonPath("$.order.externalOrderId").value(externalOrderId))
            .andExpect(jsonPath("$.order.warehouseCode").value(warehouseA));

        CustomerOrder order = customerOrderRepository
            .findByTenant_CodeIgnoreCaseAndExternalOrderId(tenantA, externalOrderId)
            .orElseThrow();
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().getFirst().getQuantity()).isEqualTo(3);
        assertThat(order.getStatus().name()).isEqualTo("RECEIVED");
        assertThat(customerOrderRepository.findAll().stream()
            .filter(candidate -> externalOrderId.equals(candidate.getExternalOrderId()))).hasSize(1);
        assertThat(fulfillmentTaskRepository
            .findByTenant_CodeIgnoreCaseAndCustomerOrder_ExternalOrderId(tenantA, externalOrderId))
            .get().extracting(task -> task.getStatus()).isEqualTo(FulfillmentStatus.QUEUED);
        assertThat(inventoryRepository.findByProductIdAndWarehouseId(
                order.getItems().getFirst().getProduct().getId(), order.getWarehouse().getId()))
            .get().extracting(item -> item.getQuantityReserved(), item -> item.getQuantityAvailable())
            .containsExactly(3L, 7L);

        var recoveredReplay = replayRecordRepository.findById(replay.getId()).orElseThrow();
        assertThat(recoveredReplay.getStatus()).isEqualTo(IntegrationReplayStatus.REPLAYED);
        assertThat(recoveredReplay.getReplayedOrderExternalId()).isEqualTo(externalOrderId);
        assertThat(inboundRecordRepository.findById(inbound.getId()).orElseThrow().getStatus())
            .isEqualTo(IntegrationInboundStatus.REPLAYED);
        assertThat(businessEventRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(tenantA))
            .anyMatch(event -> event.getEventType() == BusinessEventType.INTEGRATION_REPLAY_COMPLETED
                && event.getPayloadSummary().contains(externalOrderId));
        assertThat(auditLogRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(tenantA))
            .anyMatch(audit -> audit.getTargetRef().contains(externalOrderId));

        mockMvc.perform(post("/api/integrations/orders/replay/" + replay.getId())
                .session(integrationOperatorA))
            .andExpect(status().isBadRequest());
        awaitTenantDispatchCompletion();
        assertThat(dispatchWorkItemRepository.findAll().stream()
            .filter(item -> tenantA.equalsIgnoreCase(item.getTenantCode()))
            .filter(item -> requestId.equals(item.getRequestId()) || item.getSource().contains("integration-replay"))
            .map(item -> item.getStatus()))
            .doesNotContain(OperationalDispatchStatus.PENDING, OperationalDispatchStatus.PROCESSING);

        mockMvc.perform(get("/api/dashboard/snapshot").session(tenantAdminA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.integrationReplayQueue").isArray());
    }

    @Test
    void inventoryAndDisabledConnectorFailuresRecoverWithoutAutomatedOrDuplicateSideEffects() throws Exception {
        String missingInventoryOrder = "L2P4-MISSING-INVENTORY-" + suffix;
        importCsv(integrationOperatorA, "l2p4-csv", missingInventoryOrder, inventoryMissingSku, 2, 20.00)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("INVENTORY_NOT_FOUND"));
        var missingInventoryReplay = replayRecord(missingInventoryOrder);
        createInventory(tenantAdminA, inventoryMissingSku, warehouseA, 8);
        replaySuccessfully(integrationOperatorA, missingInventoryReplay.getId(), missingInventoryOrder);

        String insufficientOrder = "L2P4-INSUFFICIENT-" + suffix;
        importCsv(integrationOperatorA, "l2p4-csv", insufficientOrder, insufficientSku, 3, 20.00)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("INSUFFICIENT_INVENTORY"));
        var insufficientReplay = replayRecord(insufficientOrder);
        updateInventory(tenantAdminA, insufficientSku, warehouseA, 12);
        replaySuccessfully(integrationOperatorA, insufficientReplay.getId(), insufficientOrder);

        String disabledOrder = "L2P4-DISABLED-" + suffix;
        long disabledOrdersBefore = customerOrderRepository.countByTenant_CodeIgnoreCase(tenantA);
        importCsv(integrationOperatorA, "l2p4-disabled", disabledOrder, validSku, 2, 20.00)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ordersImported").value(0))
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("CONNECTOR_DISABLED"));
        var disabledReplay = replayRecord(disabledOrder);
        requestTraceContext.clear();
        assertThat(integrationReplayService.processAutomatedReplayBatch(20)).isZero();
        assertThat(replayRecordRepository.findById(disabledReplay.getId()).orElseThrow().getStatus())
            .isEqualTo(IntegrationReplayStatus.PENDING);
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(tenantA)).isEqualTo(disabledOrdersBefore);

        createConnector(integrationAdminA, "l2p4-disabled", true, warehouseA);
        requestTraceContext.clear();
        assertThat(integrationReplayService.processAutomatedReplayBatch(20)).isZero();
        replaySuccessfully(integrationOperatorA, disabledReplay.getId(), disabledOrder);
        assertThat(customerOrderRepository.findAll().stream()
            .filter(order -> disabledOrder.equals(order.getExternalOrderId()))).hasSize(1);
        assertThat(replayRecordRepository.findById(disabledReplay.getId()).orElseThrow().getStatus())
            .isEqualTo(IntegrationReplayStatus.REPLAYED);
    }

    @Test
    void automatedRecoveryAndNegativeBoundariesRemainScopedAndNonOperational() throws Exception {
        String automatedOrder = "L2P4-AUTOMATED-" + suffix;
        String automatedSku = "L2P4-AUTO-SKU-" + suffix;
        importCsv(integrationOperatorA, "l2p4-csv", automatedOrder, automatedSku, 4, 21.00)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("PRODUCT_NOT_FOUND"));
        var automatedReplay = replayRecord(automatedOrder);
        createProduct(tenantAdminA, automatedSku);
        updateInventory(tenantAdminA, automatedSku, warehouseA, 9);
        requestTraceContext.clear();
        assertThat(integrationReplayService.processAutomatedReplayBatch(20)).isEqualTo(1);
        assertThat(requestTraceContext.getCurrentActor()).isEmpty();
        assertThat(requestTraceContext.getCurrentTenant()).isEmpty();
        assertThat(requestTraceContext.getCurrentRequestId()).isEmpty();
        assertThat(replayRecordRepository.findById(automatedReplay.getId()).orElseThrow().getStatus())
            .isEqualTo(IntegrationReplayStatus.REPLAYED);
        assertThat(customerOrderRepository.findAll().stream()
            .filter(order -> automatedOrder.equals(order.getExternalOrderId()))).hasSize(1);

        String invalidOrder = "L2P4-INVALID-" + suffix;
        long ordersBeforeInvalid = customerOrderRepository.countByTenant_CodeIgnoreCase(tenantA);
        importCsv(integrationOperatorA, "l2p4-csv", invalidOrder, validSku, 0, 20.00)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ordersImported").value(0))
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("INVALID_QUANTITY"));
        assertThat(replayRecordRepository.findAll().stream()
            .noneMatch(record -> invalidOrder.equals(record.getExternalOrderId()))).isTrue();
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(tenantA)).isEqualTo(ordersBeforeInvalid);

        String scopedOrder = "L2P4-SCOPED-" + suffix;
        importCsv(integrationOperatorA, "l2p4-csv", scopedOrder, validSku + "-UNKNOWN", 1, 20.00)
            .andExpect(status().isOk());
        var scopedReplay = replayRecord(scopedOrder);
        mockMvc.perform(post("/api/integrations/orders/replay/" + scopedReplay.getId())
                .session(warehouseOperatorB))
            .andExpect(status().isForbidden());
        assertThat(replayRecordRepository.findById(scopedReplay.getId()).orElseThrow().getStatus())
            .isEqualTo(IntegrationReplayStatus.PENDING);

        mockMvc.perform(post("/api/integrations/orders/replay/" + scopedReplay.getId())
                .session(tenantBOperator))
            .andExpect(status().isNotFound());
        assertThat(customerOrderRepository.findAll().stream()
            .noneMatch(order -> scopedOrder.equals(order.getExternalOrderId()))).isTrue();
    }

    private org.springframework.test.web.servlet.ResultActions importCsv(MockHttpSession session, String source,
                                                                          String externalOrderId, String sku,
                                                                          int quantity, double unitPrice) throws Exception {
        String csv = "sourceSystem,externalOrderId,warehouseCode,productSku,quantity,unitPrice\n"
            + "%s,%s,%s,%s,%d,%.2f\n".formatted(source, externalOrderId, warehouseA, sku, quantity, unitPrice);
        MockMultipartFile file = new MockMultipartFile(
            "file", externalOrderId + ".csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        return mockMvc.perform(multipart("/api/integrations/orders/csv-import")
            .file(file).session(session).param("sourceSystem", source));
    }

    private void replaySuccessfully(MockHttpSession session, Long replayId, String externalOrderId) throws Exception {
        mockMvc.perform(post("/api/integrations/orders/replay/" + replayId).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.replay.status").value("REPLAYED"))
            .andExpect(jsonPath("$.order.externalOrderId").value(externalOrderId));
    }

    private com.synapsecore.domain.entity.IntegrationReplayRecord replayRecord(String externalOrderId) {
        return replayRecordRepository.findAll().stream()
            .filter(record -> tenantA.equalsIgnoreCase(record.getTenantCode()))
            .filter(record -> externalOrderId.equals(record.getExternalOrderId()))
            .findFirst().orElseThrow();
    }

    private com.synapsecore.domain.entity.IntegrationInboundRecord inboundRecord(String externalOrderId) {
        return inboundRecordRepository.findAll().stream()
            .filter(record -> tenantA.equalsIgnoreCase(record.getTenantCode()))
            .filter(record -> externalOrderId.equals(record.getExternalOrderId()))
            .findFirst().orElseThrow();
    }

    private void awaitTenantDispatchCompletion() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DISPATCH_TIMEOUT_MILLIS);
        while (System.nanoTime() < deadline) {
            if (!dispatchQueueService.isDraining()) {
                dispatchQueueService.processPendingWork();
            }
            boolean inFlight = dispatchWorkItemRepository.findAll().stream()
                .filter(item -> tenantA.equalsIgnoreCase(item.getTenantCode()))
                .anyMatch(item -> item.getStatus() == OperationalDispatchStatus.PENDING
                    || item.getStatus() == OperationalDispatchStatus.PROCESSING);
            if (!inFlight && !dispatchQueueService.isDraining()) {
                return;
            }
            Thread.sleep(50);
        }
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
                .content("{\"sku\":\"%s\",\"name\":\"Layer 2 Phase 4 %s\",\"category\":\"Recovery\"}"
                    .formatted(sku, sku)))
            .andExpect(status().isCreated());
    }

    private void updateInventory(MockHttpSession session, String sku, String warehouse, long quantity) throws Exception {
        mockMvc.perform(post("/api/inventory/update").session(session).contentType(APPLICATION_JSON)
                .content("{\"productSku\":\"%s\",\"warehouseCode\":\"%s\",\"quantityAvailable\":%d,\"reorderThreshold\":0}"
                    .formatted(sku, warehouse, quantity)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantityAvailable").value(quantity));
    }

    private void createInventory(MockHttpSession session, String sku, String warehouse, long quantity) throws Exception {
        updateInventory(session, sku, warehouse, quantity);
    }

    private void createConnector(MockHttpSession session, String source, boolean enabled, String warehouse) throws Exception {
        mockMvc.perform(post("/api/integrations/orders/connectors").session(session).contentType(APPLICATION_JSON)
                .content("{\"sourceSystem\":\"%s\",\"type\":\"CSV_ORDER_IMPORT\",\"displayName\":\"Layer 2 %s\",\"enabled\":%s,\"syncMode\":\"BATCH_FILE_DROP\",\"validationPolicy\":\"STANDARD\",\"transformationPolicy\":\"NORMALIZE_CODES\",\"allowDefaultWarehouseFallback\":false,\"defaultWarehouseCode\":\"%s\"}"
                    .formatted(source, source, enabled, warehouse)))
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

    private String tenantPayloadA() {
        return """
            {
              "tenantCode":"%s",
              "tenantName":"Layer 2 Phase 4 Tenant A",
              "description":"Disposable failure and replay recovery tenant.",
              "adminFullName":"Layer 2 Phase 4 Tenant A Admin",
              "adminUsername":"p4.admin.%s",
              "adminPassword":"%s",
              "primaryLocation":"Layer 2 Region A",
              "warehouses":[
                {"code":"%s","name":"Layer 2 Phase 4 Warehouse A","location":"Region A"},
                {"code":"%s","name":"Layer 2 Phase 4 Warehouse B","location":"Region B"}
              ],
              "users":[
                {"username":"p4.admin.%s","fullName":"Layer 2 Phase 4 Tenant A Admin","operatorActorName":"Layer 2 Phase 4 Tenant A Admin","roles":["TENANT_ADMIN"],"warehouseScopes":[]},
                {"username":"p4.integration.admin.%s","fullName":"Layer 2 Phase 4 Integration Admin","operatorActorName":"Layer 2 Phase 4 Integration Admin","roles":["INTEGRATION_ADMIN"],"warehouseScopes":["%s"],"initialPassword":"%s"},
                {"username":"p4.integration.operator.%s","fullName":"Layer 2 Phase 4 Integration Operator","operatorActorName":"Layer 2 Phase 4 Integration Operator","roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["%s"],"initialPassword":"%s"},
                {"username":"p4.warehouse.operator.%s","fullName":"Layer 2 Phase 4 Warehouse B Operator","operatorActorName":"Layer 2 Phase 4 Warehouse B Operator","roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["%s"],"initialPassword":"%s"}
              ],
              "requiredRoles":[]
            }
            """.formatted(tenantA, suffix, ADMIN_PASSWORD, warehouseA, warehouseB, suffix, suffix, warehouseA,
                INITIAL_PASSWORD, suffix, warehouseA, INITIAL_PASSWORD, suffix, warehouseB, INITIAL_PASSWORD);
    }

    private String tenantPayloadB() {
        return """
            {
              "tenantCode":"%s",
              "tenantName":"Layer 2 Phase 4 Tenant B",
              "description":"Disposable cross-tenant replay boundary tenant.",
              "adminFullName":"Layer 2 Phase 4 Tenant B Admin",
              "adminUsername":"p4.admin.%s",
              "adminPassword":"%s",
              "primaryLocation":"Layer 2 Region B",
              "warehouses":[{"code":"%s","name":"Layer 2 Phase 4 Tenant B Warehouse","location":"Region B"}],
              "users":[{"username":"p4.admin.%s","fullName":"Layer 2 Phase 4 Tenant B Admin","operatorActorName":"Layer 2 Phase 4 Tenant B Admin","roles":["TENANT_ADMIN"],"warehouseScopes":[]},{"username":"p4.tenantb.operator.%s","fullName":"Layer 2 Phase 4 Tenant B Operator","operatorActorName":"Layer 2 Phase 4 Tenant B Operator","roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["%s"],"initialPassword":"%s"}],
              "requiredRoles":[]
            }
            """.formatted(tenantB, suffix, ADMIN_PASSWORD, tenantBWarehouse, suffix, suffix, tenantBWarehouse,
                INITIAL_PASSWORD);
    }
}
