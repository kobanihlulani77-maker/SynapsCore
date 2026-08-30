package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.IntegrationInboundRecordRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.repository.ScenarioRunRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "synapsecore.starter.require-explicit-tenant-provisioning=true",
    "synapsecore.starter.seed-starter-inventory-on-tenant-onboarding=false",
    "synapsecore.starter.seed-starter-connectors-on-tenant-onboarding=false",
    "synapsecore.dashboard.cache-enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class Layer2Phase1OperationalFoundationIntegrationTest {

    private static final String PLATFORM_ADMIN_TOKEN = "test-only-platform-admin-token";
    private static final String TENANT_A = "L2-TENANT-A";
    private static final String TENANT_B = "L2-TENANT-B";
    private static final String WAREHOUSE_A = "L2-WH-A";
    private static final String WAREHOUSE_B = "L2-WH-B";
    private static final String WAREHOUSE_B_ONLY = "L2-WH-B-ONLY";
    private static final String ADMIN_PASSWORD = "Layer2Admin!2026";
    private static final String ROLE_INITIAL_PASSWORD = "Layer2Initial!2026";
    private static final String ROLE_PASSWORD = "Layer2Role!2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private BusinessEventRepository businessEventRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private FulfillmentTaskRepository fulfillmentTaskRepository;

    @Autowired
    private IntegrationConnectorRepository integrationConnectorRepository;

    @Autowired
    private IntegrationInboundRecordRepository integrationInboundRecordRepository;

    @Autowired
    private IntegrationReplayRecordRepository integrationReplayRecordRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private ScenarioRunRepository scenarioRunRepository;

    @Test
    void provisionedFoundationRemainsTenantScopedAndOperationallyEmpty() throws Exception {
        JsonNode tenantA = provisionTenant(tenantAPayload());
        JsonNode tenantB = provisionTenant(tenantBPayload());

        assertThat(tenantA.path("tenantCode").asText()).isEqualTo(TENANT_A);
        assertThat(tenantA.path("readiness").asText()).isEqualTo("READY");
        assertThat(arrayTexts(tenantA.path("starterWarehouseCodes")))
            .containsExactlyInAnyOrder(WAREHOUSE_A, WAREHOUSE_B);
        assertThat(tenantB.path("tenantCode").asText()).isEqualTo(TENANT_B);
        assertThat(tenantB.path("readiness").asText()).isEqualTo("READY");
        assertThat(arrayTexts(tenantB.path("starterWarehouseCodes")))
            .containsExactly(WAREHOUSE_B_ONLY);

        assertNoOperationalState(TENANT_A);
        assertNoOperationalState(TENANT_B);

        MockHttpSession tenantAdminA = login(TENANT_A, "l2.a.admin", ADMIN_PASSWORD,
            "TENANT_ADMIN", List.of());
        JsonNode onboardingAudit = readJson(get("/api/audit/recent").session(tenantAdminA));
        assertThat(onboardingAudit.findValuesAsText("action")).contains("TENANT_ONBOARDED");
        MockHttpSession integrationAdminA = login(TENANT_A, "l2.a.integration.admin", ROLE_INITIAL_PASSWORD,
            "INTEGRATION_ADMIN", List.of(WAREHOUSE_A));
        MockHttpSession integrationOperatorA = login(TENANT_A, "l2.a.integration.operator", ROLE_INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(WAREHOUSE_A));
        MockHttpSession integrationAdminB = login(TENANT_A, "l2.a.integration.admin.b", ROLE_INITIAL_PASSWORD,
            "INTEGRATION_ADMIN", List.of(WAREHOUSE_B));
        MockHttpSession integrationOperatorB = login(TENANT_A, "l2.a.integration.operator.b", ROLE_INITIAL_PASSWORD,
            "INTEGRATION_OPERATOR", List.of(WAREHOUSE_B));
        MockHttpSession reviewOwnerA = login(TENANT_A, "l2.a.review.owner", ROLE_INITIAL_PASSWORD,
            "REVIEW_OWNER", List.of(WAREHOUSE_A));
        MockHttpSession finalApproverA = login(TENANT_A, "l2.a.final.approver", ROLE_INITIAL_PASSWORD,
            "FINAL_APPROVER", List.of(WAREHOUSE_A));
        MockHttpSession escalationOwnerA = login(TENANT_A, "l2.a.escalation.owner", ROLE_INITIAL_PASSWORD,
            "ESCALATION_OWNER", List.of(WAREHOUSE_A));
        MockHttpSession tenantAdminB = login(TENANT_B, "l2.b.admin", ADMIN_PASSWORD,
            "TENANT_ADMIN", List.of());

        JsonNode productA = createProduct(tenantAdminA, "L2-SKU-A", "Layer 2 Product A");
        createProduct(tenantAdminA, "L2-SKU-B", "Layer 2 Product B");
        createProduct(tenantAdminA, "L2-SKU-C", "Layer 2 Product C");
        JsonNode productB = createProduct(tenantAdminB, "L2-SKU-A", "Tenant B Product A");
        assertThat(productA.path("id").asLong()).isNotEqualTo(productB.path("id").asLong());

        for (String sku : List.of("L2-SKU-A", "L2-SKU-B", "L2-SKU-C")) {
            updateInventory(tenantAdminA, sku, WAREHOUSE_A, 100L, 10L);
            updateInventory(tenantAdminA, sku, WAREHOUSE_B, 50L, 10L);
        }
        updateInventory(tenantAdminB, "L2-SKU-A", WAREHOUSE_B_ONLY, 40L, 5L);

        createConnector(integrationAdminA, "l2_active_a", true, WAREHOUSE_A)
            .andExpect(jsonPath("$.tenantCode").value(TENANT_A))
            .andExpect(jsonPath("$.defaultWarehouseCode").value(WAREHOUSE_A));
        createConnector(integrationAdminA, "l2_disabled_a", false, WAREHOUSE_A)
            .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/api/integrations/orders/connectors")
                .session(integrationAdminA)
                .contentType(APPLICATION_JSON)
                .content(connectorPayload("l2_wrong_b", true, WAREHOUSE_B)))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/integrations/orders/connectors")
                .session(integrationAdminB)
                .contentType(APPLICATION_JSON)
                .content(connectorPayload("l2_wrong_a", true, WAREHOUSE_A)))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/integrations/orders/connectors")
                .session(tenantAdminA)
                .contentType(APPLICATION_JSON)
                .content(connectorPayload("l2_admin_integration", true, WAREHOUSE_A)))
            .andExpect(status().isForbidden());

        JsonNode productsA = readJson(get("/api/products").session(tenantAdminA));
        JsonNode productsB = readJson(get("/api/products").session(tenantAdminB));
        assertThat(productsA.size()).isEqualTo(3);
        assertThat(productsB.size()).isEqualTo(1);
        assertThat(productsB.toString()).doesNotContain("Layer 2 Product B", "Layer 2 Product C");

        JsonNode inventoryA = readJson(get("/api/inventory").session(tenantAdminA));
        JsonNode inventoryB = readJson(get("/api/inventory").session(tenantAdminB));
        assertThat(inventoryA.size()).isEqualTo(6);
        assertThat(inventoryB.size()).isEqualTo(1);
        assertThat(fieldTexts(inventoryA, "warehouseCode"))
            .contains(WAREHOUSE_A, WAREHOUSE_B);
        assertThat(fieldTexts(inventoryB, "warehouseCode"))
            .containsExactly(WAREHOUSE_B_ONLY);
        assertThat(fieldTexts(inventoryA, "quantityReserved")).containsOnly("0");
        assertThat(fieldTexts(inventoryA, "quantityAvailable"))
            .containsOnly("100", "50");

        JsonNode dashboardA = readJson(get("/api/dashboard/summary").session(tenantAdminA));
        assertThat(dashboardA.path("totalProducts").asLong()).isEqualTo(3L);
        assertThat(dashboardA.path("totalWarehouses").asLong()).isEqualTo(2L);
        assertThat(dashboardA.path("inventoryRecordsCount").asLong()).isEqualTo(6L);
        assertThat(dashboardA.path("totalOrders").asLong()).isZero();
        assertThat(dashboardA.path("fulfillmentBacklogCount").asLong()).isZero();
        assertThat(dashboardA.path("activeAlerts").asLong()).isZero();
        assertThat(dashboardA.path("recommendationsCount").asLong()).isZero();

        JsonNode dashboardB = readJson(get("/api/dashboard/summary").session(tenantAdminB));
        assertThat(dashboardB.path("totalProducts").asLong()).isEqualTo(1L);
        assertThat(dashboardB.path("totalWarehouses").asLong()).isEqualTo(1L);
        assertThat(dashboardB.path("inventoryRecordsCount").asLong()).isEqualTo(1L);
        assertThat(dashboardB.path("totalOrders").asLong()).isZero();

        JsonNode eventsA = readJson(get("/api/events/recent").session(tenantAdminA));
        JsonNode auditA = readJson(get("/api/audit/recent").session(tenantAdminA));
        assertThat(eventsA.findValuesAsText("eventType"))
            .contains(BusinessEventType.PRODUCT_CATALOG_UPDATED.name(),
                BusinessEventType.INVENTORY_UPDATED.name(),
                BusinessEventType.INTEGRATION_CONNECTOR_UPDATED.name());
        assertThat(auditA.findValuesAsText("action"))
            .contains("PRODUCT_CREATED", "INVENTORY_UPDATED", "INTEGRATION_CONNECTOR_UPDATED");
        assertThat(auditA.toString()).doesNotContain("ROLE_PASSWORD", "ADMIN_PASSWORD");

        long productsBeforeNegativePaths = productRepository.findAllByTenant_CodeIgnoreCaseOrderByNameAsc(TENANT_A).size();
        long inventoryBeforeNegativePaths = inventoryRepository.countByTenantCode(TENANT_A);
        long connectorsBeforeNegativePaths = integrationConnectorRepository.countByTenant_CodeIgnoreCase(TENANT_A);
        long successfulAuditsBeforeNegativePaths = countSuccessfulAudits(TENANT_A);
        long eventsBeforeNegativePaths = businessEventRepository.countByTenantCodeIgnoreCaseAndCreatedAtAfter(
            TENANT_A, java.time.Instant.EPOCH);

        mockMvc.perform(put("/api/products/" + productA.path("id").asLong())
                .session(tenantAdminB)
                .contentType(APPLICATION_JSON)
                .content(productPayload("L2-SKU-A", "Cross Tenant Attempt")))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/inventory/update")
                .session(reviewOwnerA)
                .contentType(APPLICATION_JSON)
                .content(inventoryPayload("L2-SKU-A", WAREHOUSE_A, 90L, 10L)))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/inventory/update")
                .session(integrationOperatorA)
                .contentType(APPLICATION_JSON)
                .content(inventoryPayload("L2-SKU-A", WAREHOUSE_B, 90L, 10L)))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/inventory/update")
                .session(integrationOperatorB)
                .contentType(APPLICATION_JSON)
                .content(inventoryPayload("L2-SKU-A", WAREHOUSE_A, 90L, 10L)))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/products")
                .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(productPayload("L2-PLATFORM-ATTEMPT", "Platform Attempt")))
            .andExpect(status().isForbidden());

        assertThat(productRepository.findAllByTenant_CodeIgnoreCaseOrderByNameAsc(TENANT_A)).hasSize((int) productsBeforeNegativePaths);
        assertThat(inventoryRepository.countByTenantCode(TENANT_A)).isEqualTo(inventoryBeforeNegativePaths);
        assertThat(integrationConnectorRepository.countByTenant_CodeIgnoreCase(TENANT_A)).isEqualTo(connectorsBeforeNegativePaths);
        assertThat(countSuccessfulAudits(TENANT_A)).isEqualTo(successfulAuditsBeforeNegativePaths);
        assertThat(businessEventRepository.countByTenantCodeIgnoreCaseAndCreatedAtAfter(TENANT_A, java.time.Instant.EPOCH))
            .isEqualTo(eventsBeforeNegativePaths);

        assertThat(reviewOwnerA).isNotNull();
        assertThat(finalApproverA).isNotNull();
        assertThat(escalationOwnerA).isNotNull();
        assertNoPrematureOperationalState(TENANT_A);
        assertNoPrematureOperationalState(TENANT_B);
    }

    private JsonNode provisionTenant(String payload) throws Exception {
        return readJson(post("/api/access/tenants")
            .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
            .contentType(APPLICATION_JSON)
            .content(payload));
    }

    private MockHttpSession login(String tenantCode,
                                  String username,
                                  String password,
                                  String expectedRole,
                                  List<String> expectedScopes) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content(loginPayload(tenantCode, username, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.tenantCode").value(tenantCode))
            .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.hasItem(expectedRole)))
            .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        if (loginBody.path("passwordChangeRequired").asBoolean()) {
            MvcResult changed = mockMvc.perform(post("/api/auth/session/password")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}"
                        .formatted(password, ROLE_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordChangeRequired").value(false))
                .andReturn();
            session = (MockHttpSession) changed.getRequest().getSession(false);
        }

        JsonNode sessionBody = readJson(get("/api/auth/session").session(session));
        assertThat(sessionBody.path("signedIn").asBoolean()).isTrue();
        assertThat(arrayTexts(sessionBody.path("roles"))).contains(expectedRole);
        assertThat(arrayTexts(sessionBody.path("warehouseScopes"))).containsExactlyElementsOf(expectedScopes);
        return session;
    }

    private void updateInventory(MockHttpSession session,
                                 String sku,
                                 String warehouseCode,
                                 long quantity,
                                 long threshold) throws Exception {
        mockMvc.perform(post("/api/inventory/update")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content(inventoryPayload(sku, warehouseCode, quantity, threshold)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productSku").value(sku))
            .andExpect(jsonPath("$.warehouseCode").value(warehouseCode))
            .andExpect(jsonPath("$.quantityReserved").value(0))
            .andExpect(jsonPath("$.quantityAvailable").value(quantity));
    }

    private org.springframework.test.web.servlet.ResultActions createConnector(MockHttpSession session,
                                                                                String sourceSystem,
                                                                                boolean enabled,
                                                                                String warehouseCode) throws Exception {
        return mockMvc.perform(post("/api/integrations/orders/connectors")
            .session(session)
            .contentType(APPLICATION_JSON)
            .content(connectorPayload(sourceSystem, enabled, warehouseCode)))
            .andExpect(status().isOk());
    }

    private JsonNode createProduct(MockHttpSession session, String sku, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content(productPayload(sku, name)))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode readJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
        throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode readJson(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        MvcResult result = actions.andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private long countSuccessfulAudits(String tenantCode) {
        return auditLogRepository.findAll().stream()
            .filter(audit -> tenantCode.equalsIgnoreCase(audit.getTenantCode()))
            .filter(audit -> audit.getStatus() == AuditStatus.SUCCESS)
            .count();
    }

    private List<String> arrayTexts(JsonNode array) {
        List<String> values = new java.util.ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }

    private List<String> fieldTexts(JsonNode records, String fieldName) {
        List<String> values = new java.util.ArrayList<>();
        records.forEach(record -> values.add(record.path(fieldName).asText()));
        return values;
    }

    private void assertNoOperationalState(String tenantCode) {
        assertThat(productRepository.findAllByTenant_CodeIgnoreCaseOrderByNameAsc(tenantCode)).isEmpty();
        assertThat(inventoryRepository.countByTenantCode(tenantCode)).isZero();
        assertThat(integrationConnectorRepository.countByTenant_CodeIgnoreCase(tenantCode)).isZero();
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(tenantCode)).isZero();
        assertThat(fulfillmentTaskRepository.findAll().stream()
            .filter(task -> task.getTenant() != null && tenantCode.equalsIgnoreCase(task.getTenant().getCode())))
            .isEmpty();
        assertThat(alertRepository.countByTenant_CodeIgnoreCaseAndStatus(
            tenantCode, com.synapsecore.domain.entity.AlertStatus.ACTIVE)).isZero();
        assertThat(recommendationRepository.countByTenant_CodeIgnoreCaseAndStatus(
            tenantCode, com.synapsecore.domain.entity.RecommendationStatus.CURRENT)).isZero();
        assertThat(scenarioRunRepository.findTop12ByTenant_CodeIgnoreCaseOrderByCreatedAtDesc(tenantCode)).isEmpty();
        assertThat(integrationInboundRecordRepository.countByTenantCodeIgnoreCaseAndStatusIn(
            tenantCode, Set.of(com.synapsecore.domain.entity.IntegrationInboundStatus.RECEIVED,
                com.synapsecore.domain.entity.IntegrationInboundStatus.ACCEPTED,
                com.synapsecore.domain.entity.IntegrationInboundStatus.REJECTED))).isZero();
        assertThat(integrationReplayRecordRepository.countByTenantCodeIgnoreCaseAndStatusIn(
            tenantCode, Set.of(com.synapsecore.domain.entity.IntegrationReplayStatus.PENDING,
                com.synapsecore.domain.entity.IntegrationReplayStatus.REPLAYED,
                com.synapsecore.domain.entity.IntegrationReplayStatus.REPLAY_FAILED,
                com.synapsecore.domain.entity.IntegrationReplayStatus.DEAD_LETTERED))).isZero();
    }

    private void assertNoPrematureOperationalState(String tenantCode) {
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(tenantCode)).isZero();
        assertThat(fulfillmentTaskRepository.findAll().stream()
            .filter(task -> task.getTenant() != null && tenantCode.equalsIgnoreCase(task.getTenant().getCode())))
            .isEmpty();
        assertThat(integrationInboundRecordRepository.countByTenantCodeIgnoreCaseAndStatusIn(
            tenantCode, Set.of(com.synapsecore.domain.entity.IntegrationInboundStatus.RECEIVED,
                com.synapsecore.domain.entity.IntegrationInboundStatus.ACCEPTED,
                com.synapsecore.domain.entity.IntegrationInboundStatus.REJECTED))).isZero();
        assertThat(integrationReplayRecordRepository.countByTenantCodeIgnoreCaseAndStatusIn(
            tenantCode, Set.of(com.synapsecore.domain.entity.IntegrationReplayStatus.PENDING,
                com.synapsecore.domain.entity.IntegrationReplayStatus.REPLAYED,
                com.synapsecore.domain.entity.IntegrationReplayStatus.REPLAY_FAILED,
                com.synapsecore.domain.entity.IntegrationReplayStatus.DEAD_LETTERED))).isZero();
    }

    private String tenantAPayload() {
        return """
            {
              "tenantCode":"L2-TENANT-A",
              "tenantName":"Layer 2 Tenant A",
              "description":"Disposable Layer 2 integrated foundation tenant.",
              "adminFullName":"Layer 2 Tenant A Admin",
              "adminUsername":"l2.a.admin",
              "adminPassword":"Layer2Admin!2026",
              "primaryLocation":"North Test Region",
              "secondaryLocation":"South Test Region",
              "warehouses":[
                {"code":"L2-WH-A","name":"Layer 2 Warehouse A","location":"North Test Region"},
                {"code":"L2-WH-B","name":"Layer 2 Warehouse B","location":"South Test Region"}
              ],
              "users":[
                {"username":"l2.a.admin","fullName":"Layer 2 Tenant A Admin","operatorActorName":"Layer 2 Tenant A Admin","roles":["TENANT_ADMIN"],"warehouseScopes":[]},
                {"username":"l2.a.integration.admin","fullName":"Layer 2 A Integration Admin","operatorActorName":"Layer 2 A Integration Admin","roles":["INTEGRATION_ADMIN"],"warehouseScopes":["L2-WH-A"],"initialPassword":"Layer2Initial!2026"},
                {"username":"l2.a.integration.operator","fullName":"Layer 2 A Integration Operator","operatorActorName":"Layer 2 A Integration Operator","roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["L2-WH-A"],"initialPassword":"Layer2Initial!2026"},
                {"username":"l2.a.integration.admin.b","fullName":"Layer 2 B Integration Admin","operatorActorName":"Layer 2 B Integration Admin","roles":["INTEGRATION_ADMIN"],"warehouseScopes":["L2-WH-B"],"initialPassword":"Layer2Initial!2026"},
                {"username":"l2.a.integration.operator.b","fullName":"Layer 2 B Integration Operator","operatorActorName":"Layer 2 B Integration Operator","roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["L2-WH-B"],"initialPassword":"Layer2Initial!2026"},
                {"username":"l2.a.review.owner","fullName":"Layer 2 A Review Owner","operatorActorName":"Layer 2 A Review Owner","roles":["REVIEW_OWNER"],"warehouseScopes":["L2-WH-A"],"initialPassword":"Layer2Initial!2026"},
                {"username":"l2.a.final.approver","fullName":"Layer 2 A Final Approver","operatorActorName":"Layer 2 A Final Approver","roles":["FINAL_APPROVER"],"warehouseScopes":["L2-WH-A"],"initialPassword":"Layer2Initial!2026"},
                {"username":"l2.a.escalation.owner","fullName":"Layer 2 A Escalation Owner","operatorActorName":"Layer 2 A Escalation Owner","roles":["ESCALATION_OWNER"],"warehouseScopes":["L2-WH-A"],"initialPassword":"Layer2Initial!2026"}
              ],
              "requiredRoles":[]
            }
            """;
    }

    private String tenantBPayload() {
        return """
            {
              "tenantCode":"L2-TENANT-B",
              "tenantName":"Layer 2 Tenant B",
              "description":"Disposable Layer 2 isolation tenant.",
              "adminFullName":"Layer 2 Tenant B Admin",
              "adminUsername":"l2.b.admin",
              "adminPassword":"Layer2Admin!2026",
              "primaryLocation":"Isolated Test Region",
              "warehouses":[{"code":"L2-WH-B-ONLY","name":"Layer 2 Tenant B Warehouse","location":"Isolated Test Region"}],
              "users":[{"username":"l2.b.admin","fullName":"Layer 2 Tenant B Admin","operatorActorName":"Layer 2 Tenant B Admin","roles":["TENANT_ADMIN"],"warehouseScopes":[]}],
              "requiredRoles":[]
            }
            """;
    }

    private String loginPayload(String tenantCode, String username, String password) {
        return "{\"tenantCode\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"}"
            .formatted(tenantCode, username, password);
    }

    private String productPayload(String sku, String name) {
        return "{\"sku\":\"%s\",\"name\":\"%s\",\"category\":\"Layer 2 Foundation\"}"
            .formatted(sku, name);
    }

    private String inventoryPayload(String sku, String warehouseCode, long quantity, long threshold) {
        return "{\"productSku\":\"%s\",\"warehouseCode\":\"%s\",\"quantityAvailable\":%d,\"reorderThreshold\":%d}"
            .formatted(sku, warehouseCode, quantity, threshold);
    }

    private String connectorPayload(String sourceSystem, boolean enabled, String warehouseCode) {
        return """
            {
              "sourceSystem":"%s",
              "type":"WEBHOOK_ORDER",
              "displayName":"Layer 2 %s Connector",
              "enabled":%s,
              "syncMode":"REALTIME_PUSH",
              "validationPolicy":"STANDARD",
              "transformationPolicy":"NORMALIZE_CODES",
              "allowDefaultWarehouseFallback":false,
              "defaultWarehouseCode":"%s",
              "notes":"Disposable Layer 2 foundation connector."
            }
            """.formatted(sourceSystem, sourceSystem, enabled, warehouseCode);
    }
}
