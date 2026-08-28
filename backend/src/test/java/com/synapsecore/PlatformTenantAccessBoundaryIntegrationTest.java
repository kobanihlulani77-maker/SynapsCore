package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.access.BootstrapAccessService;
import com.synapsecore.access.PlatformAdministrationAccessService;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.ScenarioApprovalPolicy;
import com.synapsecore.domain.entity.ScenarioApprovalStage;
import com.synapsecore.domain.entity.ScenarioApprovalStatus;
import com.synapsecore.domain.entity.ScenarioReviewPriority;
import com.synapsecore.domain.entity.ScenarioRun;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.AccessUserRepository;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.domain.repository.ScenarioRunRepository;
import com.synapsecore.domain.entity.ScenarioRunType;
import com.synapsecore.config.SynapsePlatformOwnerProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.profiles.active=prod",
    "spring.datasource.url=jdbc:h2:mem:platformboundary;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.data.redis.url=redis://localhost:6379",
    "spring.session.store-type=none",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.session.SessionAutoConfiguration",
    "management.health.redis.enabled=false",
    "management.endpoint.health.validate-group-membership=false",
    "synapsecore.realtime.broker-mode=SIMPLE_IN_MEMORY",
    "synapsecore.bootstrap.initial-token=boundary-bootstrap-fixture",
    "synapsecore.bootstrap.platform-admin-token=boundary-automation-fixture",
    "synapsecore.platform-owner.username=platform.owner",
    "synapsecore.platform-owner.password-hash=configured-by-test-fixture",
    "synapsecore.platform-owner.display-name=Boundary Test Platform Owner",
    "synapsecore.starter.auto-seed-on-empty=false",
    "synapsecore.starter.allow-default-tenant-fallback=false",
    "synapsecore.starter.allow-tenant-admin-tenant-onboarding=false",
    "synapsecore.security.rate-limit.enabled=false"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlatformTenantAccessBoundaryIntegrationTest {

    private static final String REHEARSAL_TENANT = "ACCESS-BOUNDARY-REHEARSAL";
    private static final String ISOLATION_TENANT = "ACCESS-BOUNDARY-ISOLATION";
    private static final String TENANT_ADMIN_USERNAME = "boundary.admin";
    private static final String TENANT_ADMIN_PASSWORD = "Boundary-Admin-2026!";
    private static final String ROLE_PASSWORD = "Boundary-Role-2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private AccessOperatorRepository accessOperatorRepository;

    @Autowired
    private AccessUserRepository accessUserRepository;

    @Autowired
    private ScenarioRunRepository scenarioRunRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private FulfillmentTaskRepository fulfillmentTaskRepository;

    @Autowired
    private OperationalDispatchWorkItemRepository operationalDispatchWorkItemRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private BusinessEventRepository businessEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SynapsePlatformOwnerProperties platformOwnerProperties;

    private String warehouseA;
    private String warehouseB;

    @BeforeAll
    void provisionSyntheticAccessBoundaryRehearsal() throws Exception {
        platformOwnerProperties.setPasswordHash(passwordEncoder.encode("Boundary-Platform-2026!"));
        onboardTenant(REHEARSAL_TENANT, "Access Boundary Rehearsal", TENANT_ADMIN_USERNAME,
            TENANT_ADMIN_PASSWORD, BootstrapAccessService.BOOTSTRAP_TOKEN_HEADER, "boundary-bootstrap-fixture");
        onboardTenant(ISOLATION_TENANT, "Access Boundary Isolation", "isolation.admin",
            "Isolation-Admin-2026!", PlatformAdministrationAccessService.PLATFORM_ADMIN_TOKEN_HEADER,
            "boundary-automation-fixture");

        List<String> warehouseCodes = warehouseRepository
            .findAllByTenant_CodeIgnoreCaseOrderByNameAsc(REHEARSAL_TENANT)
            .stream()
            .map(warehouse -> warehouse.getCode())
            .toList();
        warehouseA = warehouseCodes.get(0);
        warehouseB = warehouseCodes.get(1);

        MockHttpSession admin = tenantLogin(REHEARSAL_TENANT, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PASSWORD);
        createRoleUser(admin, "boundary.tenant.admin", "TENANT_ADMIN", List.of());
        createRoleUser(admin, "boundary.review", "REVIEW_OWNER", List.of(warehouseA));
        createRoleUser(admin, "boundary.review.alt", "REVIEW_OWNER", List.of(warehouseA));
        createRoleUser(admin, "boundary.review.global", "REVIEW_OWNER", List.of());
        createRoleUser(admin, "boundary.review.b", "REVIEW_OWNER", List.of(warehouseB));
        createRoleUser(admin, "boundary.review.b.alt", "REVIEW_OWNER", List.of(warehouseB));
        createRoleUser(admin, "boundary.final", "FINAL_APPROVER", List.of(warehouseA));
        createRoleUser(admin, "boundary.final.alt", "FINAL_APPROVER", List.of(warehouseA));
        createRoleUser(admin, "boundary.final.b", "FINAL_APPROVER", List.of(warehouseB));
        createRoleUser(admin, "boundary.escalation", "ESCALATION_OWNER", List.of(warehouseA));
        createRoleUser(admin, "boundary.escalation.alt", "ESCALATION_OWNER", List.of(warehouseA));
        createRoleUser(admin, "boundary.escalation.b", "ESCALATION_OWNER", List.of(warehouseB));
        createRoleUser(admin, "boundary.integration.admin", "INTEGRATION_ADMIN", List.of(warehouseA));
        createRoleUser(admin, "boundary.integration.admin.b", "INTEGRATION_ADMIN", List.of(warehouseB));
        createRoleUser(admin, "boundary.integration.admin.all", "INTEGRATION_ADMIN", List.of());
        createRoleUser(admin, "boundary.integration.operator", "INTEGRATION_OPERATOR", List.of(warehouseA));
        createRoleUser(admin, "boundary.requester.b", "INTEGRATION_OPERATOR", List.of(warehouseB));
        createRoleUser(admin, "boundary.inactive", "INTEGRATION_OPERATOR", List.of(warehouseA));
        createRoleUser(admin, "boundary.review.inactive", "REVIEW_OWNER", List.of(warehouseA));
        var inactiveOperator = accessOperatorRepository
            .findByTenant_CodeIgnoreCaseAndActorNameIgnoreCase(REHEARSAL_TENANT, "boundary.inactive")
            .orElseThrow();
        inactiveOperator.setActive(false);
        accessOperatorRepository.save(inactiveOperator);

        mockMvc.perform(post("/api/products")
                .session(admin)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"sku":"BOUNDARY-SKU","name":"Boundary Fixture","category":"Verification"}
                    """))
            .andExpect(status().isCreated());
        updateInventory(admin, warehouseA, 12L);
        updateInventory(admin, warehouseB, 24L);
        MockHttpSession integrationAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.integration.admin.all", ROLE_PASSWORD);
        mockMvc.perform(post("/api/orders")
                .session(integrationAdmin)
                .contentType(APPLICATION_JSON)
                .content(orderPayload("BOUNDARY-WH-B-ORDER", warehouseB)))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/scenarios/order-impact")
                .session(admin)
                .contentType(APPLICATION_JSON)
                .content(orderPayload(null, warehouseB)))
            .andExpect(status().isOk());
    }

    @Test
    void operationalWriteAuthoritySeparatesSetupIntegrationAndGovernanceResponsibilities() throws Exception {
        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD);
        MockHttpSession integrationAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.integration.admin", ROLE_PASSWORD);
        MockHttpSession integrationOperator = tenantLogin(REHEARSAL_TENANT, "boundary.integration.operator", ROLE_PASSWORD);
        updateInventory(tenantAdmin, warehouseA, 500L);

        for (String username : List.of(
            "boundary.integration.admin",
            "boundary.integration.operator",
            "boundary.review",
            "boundary.final",
            "boundary.escalation"
        )) {
            MockHttpSession session = tenantLogin(REHEARSAL_TENANT, username, ROLE_PASSWORD);
            mockMvc.perform(post("/api/inventory/update")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content(inventoryPayload(warehouseA, 33L)))
                .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/inventory/receive")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content("{\"productSku\":\"BOUNDARY-SKU\",\"warehouseCode\":\"%s\",\"quantityReceived\":3}".formatted(warehouseA)))
                .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/inventory/adjust")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content("{\"productSku\":\"BOUNDARY-SKU\",\"warehouseCode\":\"%s\",\"quantityDelta\":-1,\"reason\":\"Boundary denial\"}".formatted(warehouseA)))
                .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/inventory/reconcile")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content("{\"productSku\":\"BOUNDARY-SKU\",\"warehouseCode\":\"%s\",\"countedOnHand\":31,\"note\":\"Boundary denial\"}".formatted(warehouseA)))
                .andExpect(status().isForbidden());
        }

        for (String username : List.of(
            "boundary.tenant.admin",
            "boundary.review",
            "boundary.final",
            "boundary.escalation"
        )) {
            MockHttpSession session = tenantLogin(REHEARSAL_TENANT, username, ROLE_PASSWORD);
            mockMvc.perform(post("/api/orders")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content(orderPayload("BOUNDARY-DENIED-ORDER-" + username.replace(".", "-").toUpperCase(), warehouseA)))
                .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/fulfillment/updates")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content("{\"externalOrderId\":\"BOUNDARY-WH-B-ORDER\",\"status\":\"PICKING\"}"))
                .andExpect(status().isForbidden());
        }

        mockMvc.perform(post("/api/orders")
                .session(integrationAdmin)
                .contentType(APPLICATION_JSON)
                .content(orderPayload("BOUNDARY-INTEGRATION-ORDER", warehouseA)))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/orders/BOUNDARY-INTEGRATION-ORDER/transition")
                .session(integrationOperator)
                .contentType(APPLICATION_JSON)
                .content("{\"status\":\"PROCESSING\",\"note\":\"Boundary allowed integration transition\",\"restockInventory\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PROCESSING"));
        mockMvc.perform(post("/api/fulfillment/updates")
                .session(integrationOperator)
                .contentType(APPLICATION_JSON)
                .content("{\"externalOrderId\":\"BOUNDARY-INTEGRATION-ORDER\",\"status\":\"PICKING\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fulfillmentStatus").value("PICKING"));
    }

    @Test
    void humanSessionIngestionRequiresIntegrationResponsibility() throws Exception {
        MockHttpSession integrationAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.integration.admin", ROLE_PASSWORD);
        updateInventory(tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD), warehouseA, 500L);
        mockMvc.perform(post("/api/integrations/orders/connectors")
                .session(integrationAdmin)
                .contentType(APPLICATION_JSON)
                .content(enabledConnectorPayload("boundary_ingestion_allowed", "WEBHOOK_ORDER")))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/integrations/orders/connectors")
                .session(integrationAdmin)
                .contentType(APPLICATION_JSON)
                .content(enabledConnectorPayload("boundary_csv_allowed", "CSV_ORDER_IMPORT")))
            .andExpect(status().isOk());

        for (String username : List.of(
            "boundary.tenant.admin",
            "boundary.review",
            "boundary.final",
            "boundary.escalation"
        )) {
            MockHttpSession session = tenantLogin(REHEARSAL_TENANT, username, ROLE_PASSWORD);
            mockMvc.perform(post("/api/integrations/orders/webhook")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content(webhookPayload("boundary_ingestion_allowed", "BOUNDARY-WEBHOOK-DENIED-" + username.replace(".", "-").toUpperCase())))
                .andExpect(status().isForbidden());
            mockMvc.perform(multipart("/api/integrations/orders/csv-import")
                    .file(csvFile("BOUNDARY-CSV-DENIED-" + username.replace(".", "-").toUpperCase(), "boundary_csv_allowed"))
                    .param("sourceSystem", "boundary_csv_allowed")
                    .session(session))
                .andExpect(status().isForbidden());
        }

        mockMvc.perform(post("/api/integrations/orders/webhook")
                .session(integrationAdmin)
                .contentType(APPLICATION_JSON)
                .content(webhookPayload("boundary_ingestion_allowed", "BOUNDARY-WEBHOOK-ALLOWED")))
            .andExpect(status().isCreated());
        MockHttpSession integrationOperator = tenantLogin(REHEARSAL_TENANT, "boundary.integration.operator", ROLE_PASSWORD);
        mockMvc.perform(multipart("/api/integrations/orders/csv-import")
                .file(csvFile("BOUNDARY-CSV-ALLOWED", "boundary_csv_allowed"))
                .param("sourceSystem", "boundary_csv_allowed")
                .session(integrationOperator))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ordersImported").value(1));
    }

    @Test
    void platformOwnerUsesDedicatedSessionAndTenantSessionsCannotInheritIt() throws Exception {
        mockMvc.perform(post("/api/platform/session/login")
                .contentType(APPLICATION_JSON)
                .content("{\"username\":\"platform.owner\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized());

        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PASSWORD);
        mockMvc.perform(get("/api/platform/overview").session(tenantAdmin))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/access/tenants").session(tenantAdmin))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/access/tenants")
                .session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content(tenantPayload("FORBIDDEN-TENANT", "Forbidden Tenant", "forbidden.admin", "Forbidden-Admin-2026!")))
            .andExpect(status().isForbidden());

        MockHttpSession platformSession = platformLogin();
        mockMvc.perform(get("/api/platform/session").session(platformSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.username").value("platform.owner"));

        mockMvc.perform(get("/api/platform/overview").session(platformSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenants[?(@.code == 'ACCESS-BOUNDARY-REHEARSAL')]").exists())
            .andExpect(jsonPath("$.runtime.readinessState").exists())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("payloadSummary"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("externalOrderId"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("inboundAccessToken"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("allowedOrigins"))));

        MvcResult tenantLoginOnPlatformSession = mockMvc.perform(post("/api/auth/session/login")
                .session(platformSession)
                .contentType(APPLICATION_JSON)
                .content(loginPayload(REHEARSAL_TENANT, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession convertedSession = (MockHttpSession) tenantLoginOnPlatformSession.getRequest().getSession(false);
        mockMvc.perform(get("/api/platform/overview").session(convertedSession))
            .andExpect(status().isForbidden());
    }

    @Test
    void platformLogoutAndSignedOutRequestsCannotRetainAuthority() throws Exception {
        MockHttpSession platformSession = platformLogin();

        mockMvc.perform(get("/api/dashboard/summary").session(platformSession))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/platform/session/logout").session(platformSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(false));

        mockMvc.perform(get("/api/platform/session"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(false));
        for (String endpoint : platformEndpoints()) {
            mockMvc.perform(get(endpoint))
                .andExpect(status().isForbidden());
        }
    }

    @Test
    void allSixRolesExposeOnlyTheirOwnSessionIdentityAndExpectedWorkspaceReads() throws Exception {
        for (RoleExpectation expectation : roleExpectations()) {
            MockHttpSession session = tenantLogin(REHEARSAL_TENANT, expectation.username(), ROLE_PASSWORD);
            var sessionResult = mockMvc.perform(get("/api/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signedIn").value(true))
                .andExpect(jsonPath("$.tenantCode").value(REHEARSAL_TENANT))
                .andExpect(jsonPath("$.username").value(expectation.username()))
                .andExpect(jsonPath("$.roles[0]").value(expectation.role()));
            if (expectation.tenantWide()) {
                sessionResult.andExpect(jsonPath("$.warehouseScopes").isEmpty());
            } else {
                sessionResult.andExpect(jsonPath("$.warehouseScopes[0]").value(warehouseA));
            }

            for (String endpoint : List.of(
                "/api/dashboard/summary",
                "/api/products",
                "/api/system/runtime",
                "/api/events/recent",
                "/api/audit/recent"
            )) {
                mockMvc.perform(get(endpoint).session(session))
                    .andExpect(status().isOk());
            }
        }
    }

    @Test
    void roleSessionsCannotReachPlatformOrUnrelatedAdministrativeActions() throws Exception {
        for (RoleExpectation expectation : roleExpectations()) {
            MockHttpSession session = tenantLogin(REHEARSAL_TENANT, expectation.username(), ROLE_PASSWORD);
            mockMvc.perform(get("/api/platform/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signedIn").value(false));
            for (String endpoint : platformEndpoints()) {
                mockMvc.perform(get(endpoint).session(session))
                    .andExpect(status().isForbidden());
            }
            mockMvc.perform(get("/api/access/tenants").session(session))
                .andExpect(status().isForbidden());

            if (expectation.tenantWide()) {
                mockMvc.perform(get("/api/access/admin/users").session(session))
                    .andExpect(status().isOk());
            } else {
                mockMvc.perform(get("/api/access/admin/users").session(session))
                    .andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void roleClashesDoNotGrantUnrelatedAdministrativeOrConnectorWrites() throws Exception {
        for (RoleExpectation expectation : roleExpectations()) {
            MockHttpSession session = tenantLogin(REHEARSAL_TENANT, expectation.username(), ROLE_PASSWORD);
            if (!"TENANT_ADMIN".equals(expectation.role())) {
                mockMvc.perform(post("/api/products")
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {"sku":"BOUNDARY-ROLE-CLASH-SKU","name":"Role Clash Fixture","category":"Verification"}
                            """))
                    .andExpect(status().isForbidden());
                mockMvc.perform(get("/api/access/admin/workspace").session(session))
                    .andExpect(status().isForbidden());
            }

            if (!"INTEGRATION_ADMIN".equals(expectation.role())) {
                mockMvc.perform(post("/api/integrations/orders/connectors")
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(connectorPayload("boundary_role_clash_denied")))
                    .andExpect(status().isForbidden());
            }
        }

        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD);
        mockMvc.perform(post("/api/products")
                .session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"sku":"BOUNDARY-ROLE-ADMIN-SKU","name":"Role Admin Fixture","category":"Verification"}
                    """))
            .andExpect(status().isCreated());

        MockHttpSession integrationAdmin = tenantLogin(
            REHEARSAL_TENANT,
            "boundary.integration.admin",
            ROLE_PASSWORD
        );
        mockMvc.perform(post("/api/integrations/orders/connectors")
                .session(integrationAdmin)
                .contentType(APPLICATION_JSON)
                .content(connectorPayload("boundary_role_connector")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceSystem").value("boundary_role_connector"));

        MockHttpSession integrationOperator = tenantLogin(
            REHEARSAL_TENANT,
            "boundary.integration.operator",
            ROLE_PASSWORD
        );
        mockMvc.perform(get("/api/integrations/orders/connectors").session(integrationOperator))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.sourceSystem == 'boundary_role_connector')]").exists());
    }

    @Test
    void scenarioGovernanceEnforcesAssignmentsAndApprovedSavedPlanExecution() throws Exception {
        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD);
        mockMvc.perform(post("/api/scenarios/order-impact")
                .session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content(orderPayload(null, warehouseA)))
            .andExpect(status().isOk());

        long reviewScenarioId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.PREVIEW)
            .filter(run -> warehouseA.equalsIgnoreCase(run.getWarehouseCode()))
            .findFirst()
            .orElseThrow()
            .getId();

        for (String username : List.of(
            "boundary.tenant.admin",
            "boundary.integration.admin",
            "boundary.integration.operator",
            "boundary.escalation"
        )) {
            MockHttpSession session = tenantLogin(REHEARSAL_TENANT, username, ROLE_PASSWORD);
            mockMvc.perform(post("/api/scenarios/" + reviewScenarioId + "/execute").session(session))
                .andExpect(status().isForbidden());
        }

        MockHttpSession reviewOwner = tenantLogin(REHEARSAL_TENANT, "boundary.review", ROLE_PASSWORD);
        mockMvc.perform(post("/api/scenarios/" + reviewScenarioId + "/execute").session(reviewOwner))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Only approved saved plans")));

        MockHttpSession finalApprover = tenantLogin(REHEARSAL_TENANT, "boundary.final", ROLE_PASSWORD);
        mockMvc.perform(post("/api/scenarios/" + reviewScenarioId + "/execute").session(finalApprover))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Only approved saved plans")));

        long assignedStandardPlanId = saveStandardScenarioPlan(
            tenantAdmin,
            "Boundary assigned standard approval",
            "boundary.review",
            warehouseA
        );
        MockHttpSession alternateReviewOwner = tenantLogin(REHEARSAL_TENANT, "boundary.review.alt", ROLE_PASSWORD);
        mockMvc.perform(post("/api/scenarios/" + assignedStandardPlanId + "/approve")
                .session(alternateReviewOwner)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review.alt\",\"approvalNote\":\"Wrong owner should fail\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned review owner")));
        mockMvc.perform(post("/api/scenarios/" + assignedStandardPlanId + "/approve")
                .session(reviewOwner)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review\",\"approvalNote\":\"Assigned owner approves\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/scenarios/" + assignedStandardPlanId + "/execute").session(reviewOwner))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.order.warehouseCode").value(warehouseA));

        long rejectedPlanId = saveStandardScenarioPlan(
            tenantAdmin,
            "Boundary assigned standard rejection",
            "boundary.review",
            warehouseA
        );
        mockMvc.perform(post("/api/scenarios/" + rejectedPlanId + "/reject")
                .session(alternateReviewOwner)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"reviewerName\":\"boundary.review.alt\",\"reason\":\"Wrong owner should fail\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned review owner")));
        mockMvc.perform(post("/api/scenarios/" + rejectedPlanId + "/reject")
                .session(reviewOwner)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"reviewerName\":\"boundary.review\",\"reason\":\"Assigned owner rejects\"}"))
            .andExpect(status().isOk());

        long escalatedPlanId = saveStandardScenarioPlan(
            tenantAdmin,
            "Boundary final approval assignment",
            "boundary.review",
            warehouseA
        );
        var escalatedRun = scenarioRunRepository.findById(escalatedPlanId).orElseThrow();
        escalatedRun.setApprovalPolicy(ScenarioApprovalPolicy.ESCALATED);
        escalatedRun.setApprovalStage(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);
        escalatedRun.setApprovalStatus(ScenarioApprovalStatus.PENDING_APPROVAL);
        escalatedRun.setReviewApprovedBy("boundary.review");
        escalatedRun.setReviewApprovedAt(Instant.now());
        escalatedRun.setFinalApprovalOwner("boundary.final");
        escalatedRun.setApprovalDueAt(Instant.now().plusSeconds(3600));
        scenarioRunRepository.save(escalatedRun);

        MockHttpSession alternateFinalApprover = tenantLogin(REHEARSAL_TENANT, "boundary.final.alt", ROLE_PASSWORD);
        mockMvc.perform(post("/api/scenarios/" + escalatedPlanId + "/approve")
                .session(alternateFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final.alt\",\"approvalNote\":\"Wrong final owner should fail\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned final approval owner")));
        mockMvc.perform(post("/api/scenarios/" + escalatedPlanId + "/approve")
                .session(finalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final\",\"approvalNote\":\"Assigned final owner approves\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/scenarios/" + escalatedPlanId + "/execute").session(finalApprover))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.order.warehouseCode").value(warehouseA));
    }

    @Test
    void escalationAcknowledgementRequiresAssignedEscalationOwner() throws Exception {
        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD);
        long escalatedPlanId = saveStandardScenarioPlan(
            tenantAdmin,
            "Boundary assigned escalation acknowledgement",
            "boundary.review",
            warehouseA
        );
        var escalatedRun = scenarioRunRepository.findById(escalatedPlanId).orElseThrow();
        escalatedRun.setApprovalPolicy(ScenarioApprovalPolicy.ESCALATED);
        escalatedRun.setApprovalStage(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);
        escalatedRun.setApprovalStatus(ScenarioApprovalStatus.PENDING_APPROVAL);
        escalatedRun.setReviewApprovedBy("boundary.review");
        escalatedRun.setReviewApprovedAt(Instant.now());
        escalatedRun.setFinalApprovalOwner("boundary.final");
        escalatedRun.setReviewPriority(ScenarioReviewPriority.HIGH);
        escalatedRun.setApprovalDueAt(Instant.now().minusSeconds(3600));
        escalatedRun.setSlaEscalatedTo("boundary.escalation");
        escalatedRun.setSlaEscalatedAt(Instant.now().minusSeconds(60));
        scenarioRunRepository.save(escalatedRun);

        MockHttpSession alternateEscalationOwner = tenantLogin(REHEARSAL_TENANT, "boundary.escalation.alt", ROLE_PASSWORD);
        mockMvc.perform(post("/api/scenarios/" + escalatedPlanId + "/acknowledge-escalation")
                .session(alternateEscalationOwner)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"ESCALATION_OWNER\",\"acknowledgedBy\":\"boundary.escalation.alt\",\"note\":\"Wrong owner should fail\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned escalation owner")));

        MockHttpSession escalationOwner = tenantLogin(REHEARSAL_TENANT, "boundary.escalation", ROLE_PASSWORD);
        mockMvc.perform(post("/api/scenarios/" + escalatedPlanId + "/acknowledge-escalation")
                .session(escalationOwner)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"ESCALATION_OWNER\",\"acknowledgedBy\":\"boundary.escalation\",\"note\":\"Assigned owner acknowledges\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slaAcknowledgedBy").value("boundary.escalation"));
    }

    @Test
    void disabledUsersAreRevokedAndScopeChangesApplyToExistingSessions() throws Exception {
        MockHttpSession admin = tenantLogin(REHEARSAL_TENANT, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PASSWORD);
        var user = accessUserRepository
            .findByTenant_CodeIgnoreCaseAndUsernameIgnoreCase(REHEARSAL_TENANT, "boundary.integration.operator")
            .orElseThrow();
        var operator = accessOperatorRepository
            .findByTenant_CodeIgnoreCaseAndActorNameIgnoreCase(REHEARSAL_TENANT, "boundary.integration.operator")
            .orElseThrow();

        MockHttpSession sessionProbe = tenantLogin(
            REHEARSAL_TENANT,
            "boundary.integration.operator",
            ROLE_PASSWORD
        );
        MockHttpSession protectedRequest = tenantLogin(
            REHEARSAL_TENANT,
            "boundary.integration.operator",
            ROLE_PASSWORD
        );
        updateUser(admin, user.getId(), false, operator.getActorName());
        mockMvc.perform(get("/api/auth/session").session(sessionProbe))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(false));
        mockMvc.perform(get("/api/inventory").session(protectedRequest))
            .andExpect(status().isForbidden());

        updateUser(admin, user.getId(), true, operator.getActorName());
        MockHttpSession scopeSession = tenantLogin(
            REHEARSAL_TENANT,
            "boundary.integration.operator",
            ROLE_PASSWORD
        );
        updateOperator(admin, operator.getId(), warehouseB);
        mockMvc.perform(get("/api/auth/session").session(scopeSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.warehouseScopes[0]").value(warehouseB));
        mockMvc.perform(get("/api/inventory").session(scopeSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.warehouseCode == '" + warehouseB + "')]").exists())
            .andExpect(jsonPath("$[?(@.warehouseCode == '" + warehouseA + "')]").doesNotExist());

        updateOperator(admin, operator.getId(), warehouseA);
        mockMvc.perform(post("/api/auth/session/logout").session(scopeSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(false));
        mockMvc.perform(get("/api/inventory"))
            .andExpect(status().isForbidden());
    }

    @Test
    void integrationReadVisibilityFollowsAssignedResponsibility() throws Exception {
        for (String username : List.of(
            "boundary.tenant.admin",
            "boundary.review",
            "boundary.final",
            "boundary.escalation"
        )) {
            MockHttpSession session = tenantLogin(REHEARSAL_TENANT, username, ROLE_PASSWORD);
            mockMvc.perform(get("/api/integrations/orders/connectors").session(session))
                .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/integrations/orders/imports/recent").session(session))
                .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/integrations/orders/replay-queue").session(session))
                .andExpect(status().isForbidden());
        }

        for (String username : List.of("boundary.integration.admin", "boundary.integration.operator")) {
            MockHttpSession session = tenantLogin(REHEARSAL_TENANT, username, ROLE_PASSWORD);
            mockMvc.perform(get("/api/integrations/orders/connectors").session(session))
                .andExpect(status().isOk());
            mockMvc.perform(get("/api/integrations/orders/imports/recent").session(session))
                .andExpect(status().isOk());
            mockMvc.perform(get("/api/integrations/orders/replay-queue").session(session))
                .andExpect(status().isOk());
        }

        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD);
        mockMvc.perform(get("/api/access/admin/workspace").session(tenantAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.connectors").isArray());
    }

    @Test
    void disabledWebhookFailureRemainsVisibleForAuthorizedRecovery() throws Exception {
        MockHttpSession integrationAdmin = tenantLogin(
            REHEARSAL_TENANT,
            "boundary.integration.admin",
            ROLE_PASSWORD
        );

        mockMvc.perform(post("/api/integrations/orders/connectors")
                .session(integrationAdmin)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "sourceSystem":"boundary_disabled_webhook",
                      "type":"WEBHOOK_ORDER",
                      "displayName":"Boundary Disabled Webhook",
                      "enabled":false,
                      "syncMode":"REALTIME_PUSH",
                      "validationPolicy":"RELAXED",
                      "transformationPolicy":"NORMALIZE_CODES",
                      "allowDefaultWarehouseFallback":false,
                      "defaultWarehouseCode":"%s",
                      "notes":"Synthetic disabled webhook recovery verification."
                    }
                    """.formatted(warehouseA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/api/integrations/orders/webhook")
                .session(integrationAdmin)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "sourceSystem":"boundary_disabled_webhook",
                      "externalOrderId":"BOUNDARY-WEBHOOK-FAILED",
                      "warehouseCode":"%s",
                      "customerReference":"BOUNDARY-CUSTOMER",
                      "occurredAt":"2026-08-22T10:00:00Z",
                      "items":[{"productSku":"BOUNDARY-SKU","quantity":1,"unitPrice":10.00}]
                    }
                    """.formatted(warehouseA)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("is disabled")));

        mockMvc.perform(get("/api/integrations/orders/replay-queue")
                .session(integrationAdmin)
                .param("externalOrderId", "BOUNDARY-WEBHOOK-FAILED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].externalOrderId").value("BOUNDARY-WEBHOOK-FAILED"))
            .andExpect(jsonPath("$[0].failureCode").value("CONNECTOR_DISABLED"))
            .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void warehouseScopeFiltersReadsAndDeniesWritesOutsideAssignedLane() throws Exception {
        MockHttpSession scopedOperator = tenantLogin(
            REHEARSAL_TENANT,
            "boundary.integration.operator",
            ROLE_PASSWORD
        );
        mockMvc.perform(get("/api/inventory").session(scopedOperator))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.warehouseCode == '" + warehouseA + "')]").exists())
            .andExpect(jsonPath("$[?(@.warehouseCode == '" + warehouseB + "')]").doesNotExist());
        mockMvc.perform(get("/api/warehouses").session(scopedOperator))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.code == '" + warehouseA + "')]").exists())
            .andExpect(jsonPath("$[?(@.code == '" + warehouseB + "')]").doesNotExist());
        mockMvc.perform(get("/api/dashboard/snapshot").session(scopedOperator))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inventory[?(@.warehouseCode == '" + warehouseB + "')]").doesNotExist());
        mockMvc.perform(get("/api/orders/recent").session(scopedOperator))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("BOUNDARY-WH-B-ORDER"))));
        mockMvc.perform(get("/api/fulfillment").session(scopedOperator))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("BOUNDARY-WH-B-ORDER"))));
        mockMvc.perform(post("/api/inventory/update")
                .session(scopedOperator)
                .contentType(APPLICATION_JSON)
                .content(inventoryPayload(warehouseB, 30L)))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/orders/BOUNDARY-WH-B-ORDER/transition")
                .session(scopedOperator)
                .contentType(APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\",\"note\":\"Boundary denial proof\",\"restockInventory\":true}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/fulfillment/updates")
                .session(scopedOperator)
                .contentType(APPLICATION_JSON)
                .content("{\"externalOrderId\":\"BOUNDARY-WH-B-ORDER\",\"status\":\"PICKING\"}"))
            .andExpect(status().isForbidden());

        MockHttpSession scenarioTenantAdmin = tenantLogin(
            REHEARSAL_TENANT,
            TENANT_ADMIN_USERNAME,
            TENANT_ADMIN_PASSWORD
        );
        mockMvc.perform(post("/api/scenarios/order-impact")
                .session(scenarioTenantAdmin)
                .contentType(APPLICATION_JSON)
                .content(orderPayload(null, warehouseB)))
            .andExpect(status().isOk());

        long warehouseBScenarioId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.PREVIEW)
            .filter(run -> warehouseB.equalsIgnoreCase(run.getWarehouseCode()))
            .findFirst()
            .orElseThrow()
            .getId();
        mockMvc.perform(get("/api/scenarios/history").session(scopedOperator))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + warehouseBScenarioId + ")]").doesNotExist());
        mockMvc.perform(post("/api/scenarios/" + warehouseBScenarioId + "/execute")
                .session(scopedOperator))
            .andExpect(status().isForbidden());

        MockHttpSession tenantWideAdmin = tenantLogin(REHEARSAL_TENANT, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PASSWORD);
        mockMvc.perform(get("/api/inventory").session(tenantWideAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.warehouseCode == '" + warehouseA + "')]").exists())
            .andExpect(jsonPath("$[?(@.warehouseCode == '" + warehouseB + "')]").exists());
        mockMvc.perform(get("/api/warehouses").session(tenantWideAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.code == '" + warehouseA + "')]").exists())
            .andExpect(jsonPath("$[?(@.code == '" + warehouseB + "')]").exists());
    }

    @Test
    void tenantActivityAndRuntimeStayTenantScopedWhilePlatformViewsRemainMetadataOnly() throws Exception {
        MockHttpSession rehearsalAdmin = tenantLogin(REHEARSAL_TENANT, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PASSWORD);
        MockHttpSession isolationAdmin = tenantLogin(ISOLATION_TENANT, "isolation.admin", "Isolation-Admin-2026!");

        mockMvc.perform(get("/api/events/recent").session(rehearsalAdmin))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(ISOLATION_TENANT))));
        mockMvc.perform(get("/api/events/recent").session(isolationAdmin))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(REHEARSAL_TENANT))));

        mockMvc.perform(get("/api/system/runtime").session(rehearsalAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.telemetry").exists())
            .andExpect(jsonPath("$.connectorDiagnostics").exists())
            .andExpect(jsonPath("$.activeProfiles").doesNotExist())
            .andExpect(jsonPath("$.allowedOrigins").doesNotExist())
            .andExpect(jsonPath("$.headerFallbackEnabled").doesNotExist())
            .andExpect(jsonPath("$.build.instanceId").isEmpty());

        MockHttpSession platformSession = platformLogin();
        mockMvc.perform(get("/api/platform/activity").session(platformSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].scope").exists())
            .andExpect(jsonPath("$[0].classification").exists())
            .andExpect(jsonPath("$[0].impact").exists())
            .andExpect(jsonPath("$[0].severity").exists())
            .andExpect(jsonPath("$[0].interpretation").exists())
            .andExpect(jsonPath("$[0].nextAction").exists())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("payloadSummary"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("details"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("targetRef"))));
        mockMvc.perform(get("/api/platform/runtime").session(platformSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeProfiles").exists())
            .andExpect(jsonPath("$.build.commit").exists())
            .andExpect(jsonPath("$.allowedOrigins").doesNotExist());
    }

    private List<String> platformEndpoints() {
        return List.of(
            "/api/platform/overview",
            "/api/platform/tenants",
            "/api/platform/runtime",
            "/api/platform/activity"
        );
    }

    private List<RoleExpectation> roleExpectations() {
        return List.of(
            new RoleExpectation("boundary.tenant.admin", "TENANT_ADMIN", true),
            new RoleExpectation("boundary.integration.admin", "INTEGRATION_ADMIN", false),
            new RoleExpectation("boundary.integration.operator", "INTEGRATION_OPERATOR", false),
            new RoleExpectation("boundary.review", "REVIEW_OWNER", false),
            new RoleExpectation("boundary.final", "FINAL_APPROVER", false),
            new RoleExpectation("boundary.escalation", "ESCALATION_OWNER", false)
        );
    }

    private void updateUser(MockHttpSession admin,
                            Long userId,
                            boolean active,
                            String operatorActorName) throws Exception {
        mockMvc.perform(put("/api/access/admin/users/" + userId)
                .session(admin)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"fullName":"Boundary Integration Operator","active":%s,"operatorActorName":"%s"}
                    """.formatted(active, operatorActorName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(active));
    }

    private void updateOperator(MockHttpSession admin, Long operatorId, String warehouseCode) throws Exception {
        mockMvc.perform(put("/api/access/admin/operators/" + operatorId)
                .session(admin)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"actorName":"boundary.integration.operator","displayName":"Boundary Integration Operator","description":"Synthetic boundary rehearsal role","active":true,"roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["%s"]}
                    """.formatted(warehouseCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.warehouseScopes[0]").value(warehouseCode));
    }

    private void onboardTenant(String tenantCode,
                               String tenantName,
                               String adminUsername,
                               String adminPassword,
                               String authorityHeader,
                               String authorityValue) throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .header(authorityHeader, authorityValue)
                .contentType(APPLICATION_JSON)
                .content(tenantPayload(tenantCode, tenantName, adminUsername, adminPassword)))
            .andExpect(status().isOk());
    }

    @Test
    void scenarioGovernanceRejectsRequesterSelfReviewAtSaveAndDecision() throws Exception {
        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD);
        MockHttpSession reviewOwner = tenantLogin(REHEARSAL_TENANT, "boundary.review", ROLE_PASSWORD);

        mockMvc.perform(post("/api/scenarios/save")
                .session(reviewOwner)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"title":"Boundary requester self-review save","requestedBy":"boundary.review","reviewOwner":"boundary.review","request":{"warehouseCode":"%s","items":[{"productSku":"BOUNDARY-SKU","quantity":1,"unitPrice":10.00}]}}
                    """.formatted(warehouseA)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("different from the requester")));

        long historicalSelfReviewId = saveStandardScenarioPlan(
            tenantAdmin,
            "Boundary historical requester self-review",
            "boundary.review",
            warehouseA
        );
        var historicalSelfReview = scenarioRunRepository.findById(historicalSelfReviewId).orElseThrow();
        historicalSelfReview.setRequestedBy("boundary.review");
        scenarioRunRepository.save(historicalSelfReview);

        mockMvc.perform(post("/api/scenarios/" + historicalSelfReviewId + "/approve")
                .session(reviewOwner)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"actorRole":"REVIEW_OWNER","approverName":"boundary.review","approvalNote":"Self-review must fail"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("different from the requester")));

        mockMvc.perform(post("/api/scenarios/" + historicalSelfReviewId + "/reject")
                .session(reviewOwner)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"actorRole":"REVIEW_OWNER","reviewerName":"boundary.review","reason":"Self-review must fail"}
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("different from the requester")));
    }

    @Test
    void scenarioSaveBindsRequesterToAuthenticatedSessionActor() throws Exception {
        MockHttpSession requester = tenantLogin(REHEARSAL_TENANT, "boundary.integration.operator", ROLE_PASSWORD);

        mockMvc.perform(post("/api/scenarios/save")
                .session(requester)
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(
                    "Session-bound requester",
                    "boundary.integration.operator",
                    "boundary.review",
                    warehouseA)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.requestedBy").value("boundary.integration.operator"));

        var saved = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> "Session-bound requester".equals(run.getTitle()))
            .findFirst()
            .orElseThrow();
        assertThat(saved.getRequestedBy()).isEqualTo("boundary.integration.operator");

        assertRequesterSpoofRejected(requester, "Same-tenant requester spoof", "boundary.review", warehouseA);
        assertRequesterSpoofRejected(requester, "Tenant-wide requester spoof", "Operations Lead", warehouseA);
        assertRequesterSpoofRejected(requester, "Wrong-warehouse requester spoof", "boundary.requester.b", warehouseA);
        assertRequesterSpoofRejected(requester, "Cross-tenant requester spoof", "isolation.admin", warehouseA);
        assertRequesterSpoofRejected(requester, "Inactive requester spoof", "boundary.inactive", warehouseA);
    }

    @Test
    void scenarioCreationAuthorityAllowsAllRolesWithinWarehouseScope() throws Exception {
        List<ScenarioCreationExpectation> expectations = List.of(
            new ScenarioCreationExpectation("boundary.tenant.admin", "boundary.review", warehouseA),
            new ScenarioCreationExpectation("boundary.tenant.admin", "boundary.review.b", warehouseB),
            new ScenarioCreationExpectation("boundary.integration.admin", "boundary.review", warehouseA),
            new ScenarioCreationExpectation("boundary.integration.admin.b", "boundary.review.b", warehouseB),
            new ScenarioCreationExpectation("boundary.integration.operator", "boundary.review", warehouseA),
            new ScenarioCreationExpectation("boundary.requester.b", "boundary.review.b", warehouseB),
            new ScenarioCreationExpectation("boundary.review", "boundary.review.alt", warehouseA),
            new ScenarioCreationExpectation("boundary.review.b", "boundary.review.b.alt", warehouseB),
            new ScenarioCreationExpectation("boundary.final", "boundary.review", warehouseA),
            new ScenarioCreationExpectation("boundary.final.b", "boundary.review.b", warehouseB),
            new ScenarioCreationExpectation("boundary.escalation", "boundary.review", warehouseA),
            new ScenarioCreationExpectation("boundary.escalation.b", "boundary.review.b", warehouseB)
        );

        for (ScenarioCreationExpectation expectation : expectations) {
            MockHttpSession session = tenantLogin(REHEARSAL_TENANT, expectation.username(), ROLE_PASSWORD);
            String title = "Phase 1 creation " + expectation.username() + " " + expectation.warehouseCode();

            mockMvc.perform(post("/api/scenarios/order-impact")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content(orderPayload(null, expectation.warehouseCode())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouseCode").value(expectation.warehouseCode()));

            mockMvc.perform(post("/api/scenarios/order-impact/compare")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content(scenarioComparePayload(expectation.warehouseCode())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primary.warehouseCode").value(expectation.warehouseCode()))
                .andExpect(jsonPath("$.alternative.warehouseCode").value(expectation.warehouseCode()));

            mockMvc.perform(post("/api/scenarios/save")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content(scenarioSavePayload(
                        title,
                        expectation.username(),
                        expectation.reviewOwner(),
                        expectation.warehouseCode())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestedBy").value(expectation.username()))
                .andExpect(jsonPath("$.warehouseCode").value(expectation.warehouseCode()))
                .andExpect(jsonPath("$.reviewOwner").value(expectation.reviewOwner()));
        }
    }

    @Test
    void scenarioCreationRejectsAnonymousInvalidAndWrongWarehouseRequests() throws Exception {
        mockMvc.perform(post("/api/scenarios/order-impact")
                .contentType(APPLICATION_JSON)
                .content(orderPayload(null, warehouseA)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/scenarios/order-impact/compare")
                .contentType(APPLICATION_JSON)
                .content(scenarioComparePayload(warehouseA)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/scenarios/save")
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(
                    "Phase 1 anonymous save",
                    "boundary.integration.operator",
                    "boundary.review",
                    warehouseA)))
            .andExpect(status().isForbidden());

        MockHttpSession northOperator = tenantLogin(
            REHEARSAL_TENANT,
            "boundary.integration.operator",
            ROLE_PASSWORD
        );
        mockMvc.perform(post("/api/scenarios/order-impact")
                .session(northOperator)
                .contentType(APPLICATION_JSON)
                .content(orderPayload(null, warehouseB)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/scenarios/order-impact/compare")
                .session(northOperator)
                .contentType(APPLICATION_JSON)
                .content(scenarioComparePayload(warehouseA, warehouseB)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/scenarios/save")
                .session(northOperator)
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(
                    "Phase 1 wrong warehouse save",
                    "boundary.integration.operator",
                    "boundary.review.b",
                    warehouseB)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/scenarios/order-impact")
                .session(northOperator)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"warehouseCode":"","items":[{"productSku":"BOUNDARY-SKU","quantity":1,"unitPrice":10.00}]}
                    """))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/scenarios/order-impact")
                .session(northOperator)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"warehouseCode":"%s","items":[{"productSku":"MISSING-PHASE-1-SKU","quantity":1,"unitPrice":10.00}]}
                    """.formatted(warehouseA)))
            .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/api/scenarios/order-impact")
                .session(northOperator)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"warehouseCode":"%s","items":[{"productSku":"BOUNDARY-SKU","quantity":0,"unitPrice":10.00}]}
                    """.formatted(warehouseA)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void scenarioReviewOwnerAssignmentUsesExplicitWarehouseEligibleReviewers() throws Exception {
        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD);

        mockMvc.perform(post("/api/scenarios/save")
                .session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(
                    "Bootstrap reviewer target must fail",
                    "boundary.tenant.admin",
                    "Operations Lead",
                    warehouseA)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                "explicitly assigned reviewer")));

        mockMvc.perform(post("/api/scenarios/save")
                .session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(
                    "Tenant-wide reviewer target must fail",
                    "boundary.tenant.admin",
                    "boundary.review.global",
                    warehouseA)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                "explicitly assigned reviewer")));

        mockMvc.perform(post("/api/scenarios/save")
                .session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(
                    "Wrong warehouse reviewer must fail",
                    "boundary.tenant.admin",
                    "boundary.review.b",
                    warehouseA)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/scenarios/save")
                .session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(
                    "North explicit reviewer accepted",
                    "boundary.tenant.admin",
                    "boundary.review",
                    warehouseA)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reviewOwner").value("boundary.review"));

        mockMvc.perform(post("/api/scenarios/save")
                .session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(
                    "Coast explicit reviewer accepted",
                    "boundary.tenant.admin",
                    "boundary.review.b",
                    warehouseB)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reviewOwner").value("boundary.review.b"));

        mockMvc.perform(post("/api/scenarios/save")
                .session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"title":"Coast automatic reviewer accepted","requestedBy":"boundary.tenant.admin","reviewOwner":"","request":{"warehouseCode":"%s","items":[{"productSku":"BOUNDARY-SKU","quantity":1,"unitPrice":10.00}]}}
                    """.formatted(warehouseB)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reviewOwner").value("boundary.review.b"));
    }

    @Test
    void scenarioPhaseThreeSavedPlansPreserveGovernedProposalWithoutOperationalSideEffects() throws Exception {
        Instant phaseStart = Instant.now();
        MockHttpSession northRequester = tenantLogin(
            REHEARSAL_TENANT,
            "boundary.integration.operator",
            ROLE_PASSWORD
        );
        MockHttpSession coastRequester = tenantLogin(
            REHEARSAL_TENANT,
            "boundary.requester.b",
            ROLE_PASSWORD
        );

        long startingOrders = customerOrderRepository.countByTenant_CodeIgnoreCase(REHEARSAL_TENANT);
        long startingInventory = inventoryRepository.countByTenantCode(REHEARSAL_TENANT);
        long startingAlerts = alertRepository.count();
        long startingRecommendations = recommendationRepository.count();
        long startingScenarioRuns = scenarioRunRepository.count();
        long startingBusinessEvents = businessEventRepository.count();
        long northQuantityBefore = inventoryRepository.findAllWithProductAndWarehouseByTenantCode(REHEARSAL_TENANT)
            .stream()
            .filter(inventory -> inventory.getWarehouse().getCode().equalsIgnoreCase(warehouseA))
            .filter(inventory -> inventory.getProduct().resolveCatalogSku().equalsIgnoreCase("BOUNDARY-SKU"))
            .findFirst()
            .orElseThrow()
            .getQuantityAvailable();
        long coastQuantityBefore = inventoryRepository.findAllWithProductAndWarehouseByTenantCode(REHEARSAL_TENANT)
            .stream()
            .filter(inventory -> inventory.getWarehouse().getCode().equalsIgnoreCase(warehouseB))
            .filter(inventory -> inventory.getProduct().resolveCatalogSku().equalsIgnoreCase("BOUNDARY-SKU"))
            .findFirst()
            .orElseThrow()
            .getQuantityAvailable();

        String northTitle = "Phase 3 North governed proposal";
        MvcResult northResult = mockMvc.perform(post("/api/scenarios/save")
                .session(northRequester)
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(
                    northTitle,
                    "boundary.integration.operator",
                    "boundary.review",
                    warehouseA,
                    8
                )))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("SAVED_PLAN"))
            .andExpect(jsonPath("$.warehouseCode").value(warehouseA))
            .andExpect(jsonPath("$.requestedBy").value("boundary.integration.operator"))
            .andExpect(jsonPath("$.reviewOwner").value("boundary.review"))
            .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL"))
            .andExpect(jsonPath("$.approvalStage").value("PENDING_REVIEW"))
            .andExpect(jsonPath("$.executable").value(false))
            .andExpect(jsonPath("$.reviewPriority").value("MEDIUM"))
            .andReturn();

        long northPlanId = objectMapper.readTree(northResult.getResponse().getContentAsString())
            .path("scenarioRunId")
            .asLong();
        ScenarioRun northPlan = scenarioRunRepository.findById(northPlanId).orElseThrow();
        assertThat(northPlan.getType()).isEqualTo(ScenarioRunType.SAVED_PLAN);
        assertThat(northPlan.getWarehouseCode()).isEqualTo(warehouseA);
        assertThat(northPlan.getRequestedBy()).isEqualTo("boundary.integration.operator");
        assertThat(northPlan.getReviewOwner()).isEqualTo("boundary.review");
        assertThat(northPlan.getApprovalStatus()).isEqualTo(ScenarioApprovalStatus.PENDING_APPROVAL);
        assertThat(northPlan.getApprovalStage()).isEqualTo(ScenarioApprovalStage.PENDING_REVIEW);
        assertThat(northPlan.getRequestPayload()).contains(warehouseA, "BOUNDARY-SKU", "8");
        assertThat(northPlan.getSummary()).contains("projected units", "MEDIUM");

        mockMvc.perform(get("/api/scenarios/" + northPlanId + "/request")
                .session(northRequester))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarioRunId").value(northPlanId))
            .andExpect(jsonPath("$.request.warehouseCode").value(warehouseA))
            .andExpect(jsonPath("$.request.items[0].productSku").value("BOUNDARY-SKU"))
            .andExpect(jsonPath("$.request.items[0].quantity").value(8));

        String coastTitle = "Phase 3 Coast governed proposal";
        MvcResult coastResult = mockMvc.perform(post("/api/scenarios/save")
                .session(coastRequester)
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(
                    coastTitle,
                    "boundary.requester.b",
                    "boundary.review.b",
                    warehouseB,
                    3
                )))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("SAVED_PLAN"))
            .andExpect(jsonPath("$.warehouseCode").value(warehouseB))
            .andExpect(jsonPath("$.requestedBy").value("boundary.requester.b"))
            .andExpect(jsonPath("$.reviewOwner").value("boundary.review.b"))
            .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL"))
            .andExpect(jsonPath("$.approvalStage").value("PENDING_REVIEW"))
            .andExpect(jsonPath("$.executable").value(false))
            .andReturn();

        long coastPlanId = objectMapper.readTree(coastResult.getResponse().getContentAsString())
            .path("scenarioRunId")
            .asLong();
        ScenarioRun coastPlan = scenarioRunRepository.findById(coastPlanId).orElseThrow();
        assertThat(coastPlan.getWarehouseCode()).isEqualTo(warehouseB);
        assertThat(coastPlan.getRequestedBy()).isEqualTo("boundary.requester.b");
        assertThat(coastPlan.getReviewOwner()).isEqualTo("boundary.review.b");
        assertThat(coastPlan.getRequestPayload()).contains(warehouseB, "BOUNDARY-SKU", "3");

        mockMvc.perform(get("/api/scenarios/history")
                .session(northRequester)
                .param("reviewOwner", "boundary.review"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value(northTitle))
            .andExpect(jsonPath("$[0].requestedBy").value("boundary.integration.operator"))
            .andExpect(jsonPath("$[0].warehouseCode").value(warehouseA))
            .andExpect(jsonPath("$[0].reviewOwner").value("boundary.review"))
            .andExpect(jsonPath("$[0].approvalStatus").value("PENDING_APPROVAL"))
            .andExpect(jsonPath("$[0].executable").value(false));

        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(REHEARSAL_TENANT)).isEqualTo(startingOrders);
        assertThat(inventoryRepository.countByTenantCode(REHEARSAL_TENANT)).isEqualTo(startingInventory);
        assertThat(inventoryRepository.findAllWithProductAndWarehouseByTenantCode(REHEARSAL_TENANT)
            .stream()
            .filter(inventory -> inventory.getWarehouse().getCode().equalsIgnoreCase(warehouseA))
            .filter(inventory -> inventory.getProduct().resolveCatalogSku().equalsIgnoreCase("BOUNDARY-SKU"))
            .findFirst()
            .orElseThrow()
            .getQuantityAvailable()).isEqualTo(northQuantityBefore);
        assertThat(inventoryRepository.findAllWithProductAndWarehouseByTenantCode(REHEARSAL_TENANT)
            .stream()
            .filter(inventory -> inventory.getWarehouse().getCode().equalsIgnoreCase(warehouseB))
            .filter(inventory -> inventory.getProduct().resolveCatalogSku().equalsIgnoreCase("BOUNDARY-SKU"))
            .findFirst()
            .orElseThrow()
            .getQuantityAvailable()).isEqualTo(coastQuantityBefore);
        assertThat(alertRepository.count()).isEqualTo(startingAlerts);
        assertThat(recommendationRepository.count()).isEqualTo(startingRecommendations);
        assertThat(scenarioRunRepository.count()).isEqualTo(startingScenarioRuns + 2);
        assertThat(businessEventRepository.count()).isEqualTo(startingBusinessEvents + 2);
        assertThat(businessEventRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(REHEARSAL_TENANT).stream()
            .filter(event -> event.getCreatedAt().isAfter(phaseStart))
            .toList())
            .extracting(event -> event.getEventType())
            .contains(BusinessEventType.SCENARIO_SAVED)
            .doesNotContain(BusinessEventType.SCENARIO_APPROVED, BusinessEventType.SCENARIO_EXECUTED);
    }

    @Test
    void scenarioPhaseThreeRejectsCrossTenantInactiveAndWrongRoleReviewers() throws Exception {
        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD);

        mockMvc.perform(post("/api/scenarios/save")
                .session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(
                    "Cross tenant reviewer must fail",
                    "boundary.tenant.admin",
                    "isolation.admin",
                    warehouseA
                )))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/scenarios/save")
                .session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(
                    "Missing warehouse must fail",
                    "boundary.tenant.admin",
                    "boundary.review",
                    ""
                )))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/scenarios/save")
                .session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(
                    "Inactive reviewer must fail",
                    "boundary.tenant.admin",
                    "boundary.inactive",
                    warehouseA
                )))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/scenarios/save")
                .session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(
                    "Wrong role reviewer must fail",
                    "boundary.tenant.admin",
                    "boundary.final",
                    warehouseA
                )))
            .andExpect(status().isBadRequest());
    }

    @Test
    void scenarioPhaseFourReviewHandoffRequiresPersistedAssignmentAndPreservesOperationalTruth() throws Exception {
        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD);
        MockHttpSession assignedReviewer = tenantLogin(REHEARSAL_TENANT, "boundary.review", ROLE_PASSWORD);
        MockHttpSession alternateReviewer = tenantLogin(REHEARSAL_TENANT, "boundary.review.alt", ROLE_PASSWORD);
        MockHttpSession wrongWarehouseReviewer = tenantLogin(REHEARSAL_TENANT, "boundary.review.b", ROLE_PASSWORD);
        MockHttpSession crossTenantReviewer = tenantLogin(ISOLATION_TENANT, "isolation.admin", "Isolation-Admin-2026!");

        long startingOrders = customerOrderRepository.countByTenant_CodeIgnoreCase(REHEARSAL_TENANT);
        long startingInventory = inventoryRepository.countByTenantCode(REHEARSAL_TENANT);
        long startingAlerts = alertRepository.count();
        long startingRecommendations = recommendationRepository.count();
        long startingBusinessEvents = businessEventRepository.count();

        String title = "Phase 4 review handoff North";
        long planId = saveStandardScenarioPlan(
            tenantAdmin,
            title,
            "boundary.review",
            warehouseA
        );

        ScenarioRun persistedPlan = scenarioRunRepository.findById(planId).orElseThrow();
        assertThat(persistedPlan.getType()).isEqualTo(ScenarioRunType.SAVED_PLAN);
        assertThat(persistedPlan.getRequestedBy()).isEqualTo("boundary.tenant.admin");
        assertThat(persistedPlan.getWarehouseCode()).isEqualTo(warehouseA);
        assertThat(persistedPlan.getReviewOwner()).isEqualTo("boundary.review");
        assertThat(persistedPlan.getApprovalStatus()).isEqualTo(ScenarioApprovalStatus.PENDING_APPROVAL);
        assertThat(persistedPlan.getApprovalStage()).isEqualTo(ScenarioApprovalStage.PENDING_REVIEW);

        mockMvc.perform(get("/api/scenarios/history")
                .session(assignedReviewer)
                .param("reviewOwner", "boundary.review"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + planId + ")].title").value(org.hamcrest.Matchers.hasItem(title)))
            .andExpect(jsonPath("$[?(@.id == " + planId + ")].requestedBy").value(org.hamcrest.Matchers.hasItem("boundary.tenant.admin")))
            .andExpect(jsonPath("$[?(@.id == " + planId + ")].warehouseCode").value(org.hamcrest.Matchers.hasItem(warehouseA)))
            .andExpect(jsonPath("$[?(@.id == " + planId + ")].reviewOwner").value(org.hamcrest.Matchers.hasItem("boundary.review")))
            .andExpect(jsonPath("$[?(@.id == " + planId + ")].approvalStage").value(org.hamcrest.Matchers.hasItem("PENDING_REVIEW")))
            .andExpect(jsonPath("$[?(@.id == " + planId + ")].executable").value(org.hamcrest.Matchers.hasItem(false)));

        mockMvc.perform(get("/api/scenarios/" + planId + "/request").session(assignedReviewer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarioRunId").value(planId))
            .andExpect(jsonPath("$.request.warehouseCode").value(warehouseA))
            .andExpect(jsonPath("$.request.items[0].productSku").value("BOUNDARY-SKU"));

        mockMvc.perform(get("/api/scenarios/" + planId + "/request").session(alternateReviewer))
            .andExpect(status().isOk());

        String coastTitle = "Phase 4 review handoff Coast";
        long coastPlanId = saveStandardScenarioPlan(
            tenantAdmin,
            coastTitle,
            "boundary.review.b",
            warehouseB
        );
        MockHttpSession assignedCoastReviewer = tenantLogin(REHEARSAL_TENANT, "boundary.review.b", ROLE_PASSWORD);
        mockMvc.perform(get("/api/scenarios/history")
                .session(assignedCoastReviewer)
                .param("reviewOwner", "boundary.review.b"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + coastPlanId + ")].title").value(org.hamcrest.Matchers.hasItem(coastTitle)))
            .andExpect(jsonPath("$[?(@.id == " + coastPlanId + ")].warehouseCode").value(org.hamcrest.Matchers.hasItem(warehouseB)))
            .andExpect(jsonPath("$[?(@.id == " + coastPlanId + ")].reviewOwner").value(org.hamcrest.Matchers.hasItem("boundary.review.b")))
            .andExpect(jsonPath("$[?(@.id == " + coastPlanId + ")].approvalStage").value(org.hamcrest.Matchers.hasItem("PENDING_REVIEW")));

        mockMvc.perform(get("/api/scenarios/" + coastPlanId + "/request").session(assignedCoastReviewer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarioRunId").value(coastPlanId))
            .andExpect(jsonPath("$.request.warehouseCode").value(warehouseB))
            .andExpect(jsonPath("$.request.items[0].productSku").value("BOUNDARY-SKU"));

        mockMvc.perform(get("/api/scenarios/" + coastPlanId + "/request").session(assignedReviewer))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/scenarios/" + coastPlanId + "/approve")
                .session(assignedReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review\",\"approvalNote\":\"Wrong warehouse must fail\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/scenarios/" + planId + "/approve")
                .session(alternateReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review.alt\",\"approvalNote\":\"Unassigned reviewer must fail\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned review owner")));

        mockMvc.perform(get("/api/scenarios/" + planId + "/request").session(wrongWarehouseReviewer))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/scenarios/" + planId + "/approve")
                .session(wrongWarehouseReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review.b\",\"approvalNote\":\"Wrong warehouse must fail\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/scenarios/" + planId + "/request").session(crossTenantReviewer))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/scenarios/" + planId + "/approve")
                .session(crossTenantReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review\",\"approvalNote\":\"Cross tenant must fail\"}"))
            .andExpect(status().isNotFound());

        for (String username : List.of(
            "boundary.tenant.admin",
            "boundary.integration.admin",
            "boundary.integration.operator",
            "boundary.final",
            "boundary.escalation"
        )) {
            MockHttpSession session = tenantLogin(REHEARSAL_TENANT, username, ROLE_PASSWORD);
            mockMvc.perform(post("/api/scenarios/" + planId + "/approve")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"" + username + "\",\"approvalNote\":\"Wrong role must fail\"}"))
                .andExpect(status().isForbidden());
        }

        MockHttpSession anonymousRequest = new MockHttpSession();
        mockMvc.perform(post("/api/scenarios/" + planId + "/approve")
                .session(anonymousRequest)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review\",\"approvalNote\":\"Anonymous must fail\"}"))
            .andExpect(status().isForbidden());

        MockHttpSession inactiveReviewer = tenantLogin(REHEARSAL_TENANT, "boundary.review.inactive", ROLE_PASSWORD);
        var inactiveReviewOperator = accessOperatorRepository
            .findByTenant_CodeIgnoreCaseAndActorNameIgnoreCase(REHEARSAL_TENANT, "boundary.review.inactive")
            .orElseThrow();
        long inactivePlanId = saveStandardScenarioPlan(
            tenantAdmin,
            "Phase 4 inactive assigned reviewer",
            "boundary.review.inactive",
            warehouseA
        );
        inactiveReviewOperator.setActive(false);
        accessOperatorRepository.save(inactiveReviewOperator);
        try {
            mockMvc.perform(post("/api/scenarios/" + inactivePlanId + "/approve")
                    .session(inactiveReviewer)
                    .contentType(APPLICATION_JSON)
                    .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review.inactive\",\"approvalNote\":\"Inactive reviewer must fail\"}"))
                .andExpect(status().isForbidden());
        } finally {
            inactiveReviewOperator.setActive(true);
            accessOperatorRepository.save(inactiveReviewOperator);
        }

        mockMvc.perform(post("/api/scenarios/order-impact")
                .session(tenantAdmin)
                .contentType(APPLICATION_JSON)
                .content(orderPayload(null, warehouseA)))
            .andExpect(status().isOk());
        long previewId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.PREVIEW)
            .filter(run -> warehouseA.equalsIgnoreCase(run.getWarehouseCode()))
            .findFirst()
            .orElseThrow()
            .getId();
        mockMvc.perform(post("/api/scenarios/" + previewId + "/approve")
                .session(assignedReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review\",\"approvalNote\":\"Preview must not be reviewable\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Only saved plans")));

        long missingAssignmentPlanId = saveStandardScenarioPlan(
            tenantAdmin,
            "Phase 4 missing persisted reviewer",
            "boundary.review",
            warehouseA
        );
        ScenarioRun missingAssignmentPlan = scenarioRunRepository.findById(missingAssignmentPlanId).orElseThrow();
        missingAssignmentPlan.setReviewOwner(null);
        scenarioRunRepository.save(missingAssignmentPlan);
        mockMvc.perform(post("/api/scenarios/" + missingAssignmentPlanId + "/approve")
                .session(assignedReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review\",\"approvalNote\":\"Missing assignment must fail\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned review owner")));

        assertThat(scenarioRunRepository.findById(planId).orElseThrow().getApprovalStage())
            .isEqualTo(ScenarioApprovalStage.PENDING_REVIEW);
        assertThat(scenarioRunRepository.findById(planId).orElseThrow().getApprovalStatus())
            .isEqualTo(ScenarioApprovalStatus.PENDING_APPROVAL);
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(REHEARSAL_TENANT)).isEqualTo(startingOrders);
        assertThat(inventoryRepository.countByTenantCode(REHEARSAL_TENANT)).isEqualTo(startingInventory);
        assertThat(alertRepository.count()).isEqualTo(startingAlerts);
        assertThat(recommendationRepository.count()).isEqualTo(startingRecommendations);
        assertThat(businessEventRepository.count()).isEqualTo(startingBusinessEvents + 5);
        assertThat(businessEventRepository.findTop20ByOrderByCreatedAtDesc())
            .extracting(event -> event.getEventType())
            .doesNotContain(BusinessEventType.SCENARIO_APPROVED, BusinessEventType.SCENARIO_REJECTED,
                BusinessEventType.SCENARIO_EXECUTED);
    }

    @Test
    void scenarioPhaseFiveStandardApprovalIsAssignedGovernanceOnlyAndCannotBypassEscalation() throws Exception {
        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD);
        MockHttpSession northReviewer = tenantLogin(REHEARSAL_TENANT, "boundary.review", ROLE_PASSWORD);
        MockHttpSession coastReviewer = tenantLogin(REHEARSAL_TENANT, "boundary.review.b", ROLE_PASSWORD);

        long startingOrders = customerOrderRepository.countByTenant_CodeIgnoreCase(REHEARSAL_TENANT);
        long startingInventory = inventoryRepository.countByTenantCode(REHEARSAL_TENANT);
        long startingFulfillment = fulfillmentTaskRepository.count();
        long startingDispatch = operationalDispatchWorkItemRepository.count();
        long startingAlerts = alertRepository.count();
        long startingRecommendations = recommendationRepository.count();

        long northPlanId = saveStandardScenarioPlan(
            tenantAdmin,
            "Phase 5 standard North approval",
            "boundary.review",
            warehouseA
        );
        ScenarioRun northBefore = scenarioRunRepository.findById(northPlanId).orElseThrow();
        assertThat(northBefore.getApprovalPolicy()).isEqualTo(ScenarioApprovalPolicy.STANDARD);
        assertThat(northBefore.getApprovalStage()).isEqualTo(ScenarioApprovalStage.PENDING_REVIEW);
        assertThat(northBefore.getApprovalStatus()).isEqualTo(ScenarioApprovalStatus.PENDING_APPROVAL);

        mockMvc.perform(post("/api/scenarios/" + northPlanId + "/approve")
                .session(northReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review\",\"approvalNote\":\"North standard review accepted\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
            .andExpect(jsonPath("$.approvalPolicy").value("STANDARD"))
            .andExpect(jsonPath("$.approvalStage").value("APPROVED"))
            .andExpect(jsonPath("$.approvedBy").value("boundary.review"))
            .andExpect(jsonPath("$.approvalNote").value("North standard review accepted"))
            .andExpect(jsonPath("$.executionReady").value(true));

        ScenarioRun northAfter = scenarioRunRepository.findById(northPlanId).orElseThrow();
        assertThat(northAfter.getApprovedBy()).isEqualTo("boundary.review");
        assertThat(northAfter.getApprovalStage()).isEqualTo(ScenarioApprovalStage.APPROVED);
        assertThat(northAfter.getApprovalStatus()).isEqualTo(ScenarioApprovalStatus.APPROVED);
        assertThat(northAfter.getReviewApprovedBy()).isNull();
        assertThat(northAfter.getApprovalNote()).isEqualTo("North standard review accepted");

        long eventsAfterNorthApproval = businessEventRepository.count();
        mockMvc.perform(post("/api/scenarios/" + northPlanId + "/approve")
                .session(northReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review\",\"approvalNote\":\"Duplicate approval must be safe\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
            .andExpect(jsonPath("$.approvedBy").value("boundary.review"));
        assertThat(businessEventRepository.count()).isEqualTo(eventsAfterNorthApproval);

        mockMvc.perform(post("/api/scenarios/" + northPlanId + "/reject")
                .session(northReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"reviewerName\":\"boundary.review\",\"reason\":\"Contradictory terminal decision\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already been approved")));

        long coastPlanId = saveStandardScenarioPlan(
            tenantAdmin,
            "Phase 5 standard Coast approval",
            "boundary.review.b",
            warehouseB
        );
        mockMvc.perform(post("/api/scenarios/" + coastPlanId + "/approve")
                .session(coastReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review.b\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
            .andExpect(jsonPath("$.approvalPolicy").value("STANDARD"))
            .andExpect(jsonPath("$.approvalStage").value("APPROVED"))
            .andExpect(jsonPath("$.approvedBy").value("boundary.review.b"));
        assertThat(scenarioRunRepository.findById(coastPlanId).orElseThrow().getApprovalNote()).isNull();

        long escalatedPlanId = saveStandardScenarioPlan(
            tenantAdmin,
            "Phase 5 escalated review boundary",
            "boundary.review",
            warehouseA
        );
        ScenarioRun escalatedPlan = scenarioRunRepository.findById(escalatedPlanId).orElseThrow();
        escalatedPlan.setApprovalPolicy(ScenarioApprovalPolicy.ESCALATED);
        escalatedPlan.setReviewPriority(ScenarioReviewPriority.HIGH);
        escalatedPlan.setApprovalStage(ScenarioApprovalStage.PENDING_REVIEW);
        escalatedPlan.setApprovalStatus(ScenarioApprovalStatus.PENDING_APPROVAL);
        escalatedPlan.setFinalApprovalOwner("boundary.final");
        escalatedPlan.setApprovalDueAt(Instant.now().plusSeconds(3600));
        scenarioRunRepository.save(escalatedPlan);

        mockMvc.perform(post("/api/scenarios/" + escalatedPlanId + "/approve")
                .session(northReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review\",\"approvalNote\":\"North owner review complete\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL"))
            .andExpect(jsonPath("$.approvalPolicy").value("ESCALATED"))
            .andExpect(jsonPath("$.approvalStage").value("PENDING_FINAL_APPROVAL"))
            .andExpect(jsonPath("$.reviewApprovedBy").value("boundary.review"))
            .andExpect(jsonPath("$.finalApprovalOwner").value("boundary.final"))
            .andExpect(jsonPath("$.executionReady").value(false));
        ScenarioRun escalatedAfter = scenarioRunRepository.findById(escalatedPlanId).orElseThrow();
        assertThat(escalatedAfter.getApprovalStatus()).isEqualTo(ScenarioApprovalStatus.PENDING_APPROVAL);
        assertThat(escalatedAfter.getApprovalStage()).isEqualTo(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);
        assertThat(escalatedAfter.getApprovedBy()).isNull();

        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(REHEARSAL_TENANT)).isEqualTo(startingOrders);
        assertThat(inventoryRepository.countByTenantCode(REHEARSAL_TENANT)).isEqualTo(startingInventory);
        assertThat(fulfillmentTaskRepository.count()).isEqualTo(startingFulfillment);
        assertThat(operationalDispatchWorkItemRepository.count()).isEqualTo(startingDispatch);
        assertThat(alertRepository.count()).isEqualTo(startingAlerts);
        assertThat(recommendationRepository.count()).isEqualTo(startingRecommendations);

        var recentEvents = businessEventRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(REHEARSAL_TENANT);
        assertThat(recentEvents).extracting(event -> event.getEventType())
            .contains(BusinessEventType.SCENARIO_APPROVED, BusinessEventType.SCENARIO_ESCALATION_ADVANCED)
            .doesNotContain(BusinessEventType.SCENARIO_EXECUTED);
        assertThat(recentEvents.stream()
            .filter(event -> event.getEventType() == BusinessEventType.SCENARIO_APPROVED)
            .map(event -> event.getPayloadSummary())
            .anyMatch(summary -> summary.contains("boundary.review")))
            .isTrue();
    }

    @Test
    void scenarioPhaseSixFinalApprovalRequiresAssignedIndependentApproverAndPreservesOperationalTruth() throws Exception {
        Instant phaseStart = Instant.now();
        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD);
        MockHttpSession northReviewer = tenantLogin(REHEARSAL_TENANT, "boundary.review", ROLE_PASSWORD);
        MockHttpSession coastReviewer = tenantLogin(REHEARSAL_TENANT, "boundary.review.b", ROLE_PASSWORD);
        MockHttpSession northFinalApprover = tenantLogin(REHEARSAL_TENANT, "boundary.final", ROLE_PASSWORD);
        MockHttpSession alternateNorthFinalApprover = tenantLogin(REHEARSAL_TENANT, "boundary.final.alt", ROLE_PASSWORD);
        MockHttpSession coastFinalApprover = tenantLogin(REHEARSAL_TENANT, "boundary.final.b", ROLE_PASSWORD);
        MockHttpSession crossTenantAdmin = tenantLogin(ISOLATION_TENANT, "isolation.admin", "Isolation-Admin-2026!");

        long startingOrders = customerOrderRepository.countByTenant_CodeIgnoreCase(REHEARSAL_TENANT);
        long startingInventory = inventoryRepository.countByTenantCode(REHEARSAL_TENANT);
        long startingFulfillment = fulfillmentTaskRepository.count();
        long startingDispatch = operationalDispatchWorkItemRepository.count();
        long startingAlerts = alertRepository.count();
        long startingRecommendations = recommendationRepository.count();

        long northPlanId = saveEscalatedScenarioPlan(
            tenantAdmin,
            "Phase 6 escalated North approval",
            "boundary.review",
            "boundary.final",
            warehouseA
        );
        ScenarioRun northBeforeReview = scenarioRunRepository.findById(northPlanId).orElseThrow();
        assertThat(northBeforeReview.getApprovalPolicy()).isEqualTo(ScenarioApprovalPolicy.ESCALATED);
        assertThat(northBeforeReview.getApprovalStage()).isEqualTo(ScenarioApprovalStage.PENDING_REVIEW);
        assertThat(northBeforeReview.getApprovalStatus()).isEqualTo(ScenarioApprovalStatus.PENDING_APPROVAL);
        assertThat(northBeforeReview.getFinalApprovalOwner()).isEqualTo("boundary.final");

        mockMvc.perform(post("/api/scenarios/" + northPlanId + "/approve")
                .session(northFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final\",\"approvalNote\":\"Too early\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requires actor role REVIEW_OWNER")));

        mockMvc.perform(post("/api/scenarios/" + northPlanId + "/approve")
                .session(northReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review\",\"approvalNote\":\"North review complete\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalPolicy").value("ESCALATED"))
            .andExpect(jsonPath("$.approvalStage").value("PENDING_FINAL_APPROVAL"))
            .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL"))
            .andExpect(jsonPath("$.reviewApprovedBy").value("boundary.review"))
            .andExpect(jsonPath("$.approvedBy").doesNotExist())
            .andExpect(jsonPath("$.executionReady").value(false));

        for (String username : List.of("boundary.tenant.admin", "boundary.integration.admin",
            "boundary.integration.operator", "boundary.escalation")) {
            MockHttpSession session = tenantLogin(REHEARSAL_TENANT, username, ROLE_PASSWORD);
            mockMvc.perform(post("/api/scenarios/" + northPlanId + "/approve")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"" + username
                        + "\",\"approvalNote\":\"Wrong role\"}"))
                .andExpect(status().isForbidden());
        }

        mockMvc.perform(post("/api/scenarios/" + northPlanId + "/approve")
                .session(northReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review\",\"approvalNote\":\"Review owner cannot final approve\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requires actor role FINAL_APPROVER")));

        mockMvc.perform(post("/api/scenarios/" + northPlanId + "/approve")
                .session(alternateNorthFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final.alt\",\"approvalNote\":\"Unassigned final owner\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned final approval owner")));

        MockHttpSession wrongWarehouseFinalApprover = tenantLogin(REHEARSAL_TENANT, "boundary.final.b", ROLE_PASSWORD);
        mockMvc.perform(post("/api/scenarios/" + northPlanId + "/approve")
                .session(wrongWarehouseFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final.b\",\"approvalNote\":\"Wrong warehouse\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/scenarios/" + northPlanId + "/approve")
                .session(crossTenantAdmin)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final\",\"approvalNote\":\"Cross tenant\"}"))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/scenarios/" + northPlanId + "/approve")
                .session(northFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final.alt\",\"approvalNote\":\"Actor spoof\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/scenarios/" + northPlanId + "/approve")
                .session(northFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final\",\"approvalNote\":\"North final approval\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalPolicy").value("ESCALATED"))
            .andExpect(jsonPath("$.approvalStage").value("APPROVED"))
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
            .andExpect(jsonPath("$.approvedBy").value("boundary.final"))
            .andExpect(jsonPath("$.executionReady").value(true));

        long eventsAfterNorthFinalApproval = businessEventRepository.count();
        mockMvc.perform(post("/api/scenarios/" + northPlanId + "/approve")
                .session(northFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final\",\"approvalNote\":\"Duplicate final approval\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvedBy").value("boundary.final"));
        assertThat(businessEventRepository.count()).isEqualTo(eventsAfterNorthFinalApproval);

        mockMvc.perform(post("/api/scenarios/" + northPlanId + "/reject")
                .session(northFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"reviewerName\":\"boundary.final\",\"reason\":\"Contradictory final decision\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already been approved")));

        ScenarioRun northAfter = scenarioRunRepository.findById(northPlanId).orElseThrow();
        assertThat(northAfter.getReviewApprovedBy()).isEqualTo("boundary.review");
        assertThat(northAfter.getApprovedBy()).isEqualTo("boundary.final");
        assertThat(northAfter.getApprovalStage()).isEqualTo(ScenarioApprovalStage.APPROVED);

        long coastPlanId = saveEscalatedScenarioPlan(
            tenantAdmin,
            "Phase 6 escalated Coast approval",
            "boundary.review.b",
            "boundary.final.b",
            warehouseB
        );
        mockMvc.perform(post("/api/scenarios/" + coastPlanId + "/approve")
                .session(coastReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review.b\",\"approvalNote\":\"Coast review complete\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStage").value("PENDING_FINAL_APPROVAL"));
        mockMvc.perform(post("/api/scenarios/" + coastPlanId + "/approve")
                .session(northFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final\",\"approvalNote\":\"Wrong Coast final owner\"}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/scenarios/" + coastPlanId + "/approve")
                .session(coastFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final.b\",\"approvalNote\":\"Coast final approval\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStage").value("APPROVED"))
            .andExpect(jsonPath("$.approvedBy").value("boundary.final.b"));

        long standardPlanId = saveStandardScenarioPlan(
            tenantAdmin,
            "Phase 6 standard already approved boundary",
            "boundary.review",
            warehouseA
        );
        mockMvc.perform(post("/api/scenarios/" + standardPlanId + "/approve")
                .session(northReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"approverName\":\"boundary.review\",\"approvalNote\":\"Standard approval\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/scenarios/" + standardPlanId + "/approve")
                .session(northFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final\",\"approvalNote\":\"Must not approve standard\"}"))
            .andExpect(status().isBadRequest());

        long rejectedPlanId = saveEscalatedScenarioPlan(
            tenantAdmin,
            "Phase 6 rejected final boundary",
            "boundary.review",
            "boundary.final",
            warehouseA
        );
        mockMvc.perform(post("/api/scenarios/" + rejectedPlanId + "/reject")
                .session(northReviewer)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"REVIEW_OWNER\",\"reviewerName\":\"boundary.review\",\"reason\":\"Rejected before final review\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/scenarios/" + rejectedPlanId + "/approve")
                .session(northFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final\",\"approvalNote\":\"Rejected plan\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already been rejected")));

        long previewPlanId = createPreviewScenario(tenantAdmin, warehouseA);
        mockMvc.perform(post("/api/scenarios/" + previewPlanId + "/approve")
                .session(northFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final\",\"approvalNote\":\"Preview\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Only saved plans")));

        long missingAssignmentPlanId = saveEscalatedScenarioPlan(
            tenantAdmin,
            "Phase 6 missing final assignment",
            "boundary.review",
            "boundary.final",
            warehouseA
        );
        ScenarioRun missingAssignment = scenarioRunRepository.findById(missingAssignmentPlanId).orElseThrow();
        missingAssignment.setApprovalStage(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);
        missingAssignment.setFinalApprovalOwner(null);
        missingAssignment.setReviewApprovedBy("boundary.review");
        missingAssignment.setReviewApprovedAt(Instant.now());
        scenarioRunRepository.save(missingAssignment);
        mockMvc.perform(post("/api/scenarios/" + missingAssignmentPlanId + "/approve")
                .session(northFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final\",\"approvalNote\":\"Missing assignment\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned final approval owner")));

        long missingNotePlanId = saveEscalatedScenarioPlan(
            tenantAdmin,
            "Phase 6 missing final note",
            "boundary.review",
            "boundary.final",
            warehouseA
        );
        ScenarioRun missingNote = scenarioRunRepository.findById(missingNotePlanId).orElseThrow();
        missingNote.setApprovalStage(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);
        missingNote.setReviewApprovedBy("boundary.review");
        missingNote.setReviewApprovedAt(Instant.now());
        scenarioRunRepository.save(missingNote);
        mockMvc.perform(post("/api/scenarios/" + missingNotePlanId + "/approve")
                .session(northFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final\",\"approvalNote\":\"   \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requires an approval note")));

        MockHttpSession inactiveFinalApprover = null;
        createRoleUser(tenantAdmin, "boundary.final.inactive", "FINAL_APPROVER", List.of(warehouseA));
        inactiveFinalApprover = tenantLogin(REHEARSAL_TENANT, "boundary.final.inactive", ROLE_PASSWORD);
        long inactiveFinalPlanId = saveEscalatedScenarioPlan(
            tenantAdmin,
            "Phase 6 inactive final approver",
            "boundary.review",
            "boundary.final.inactive",
            warehouseA
        );
        ScenarioRun inactiveFinalPlan = scenarioRunRepository.findById(inactiveFinalPlanId).orElseThrow();
        inactiveFinalPlan.setApprovalStage(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);
        inactiveFinalPlan.setReviewApprovedBy("boundary.review");
        inactiveFinalPlan.setReviewApprovedAt(Instant.now());
        scenarioRunRepository.save(inactiveFinalPlan);
        var inactiveFinalOperator = accessOperatorRepository
            .findByTenant_CodeIgnoreCaseAndActorNameIgnoreCase(REHEARSAL_TENANT, "boundary.final.inactive")
            .orElseThrow();
        inactiveFinalOperator.setActive(false);
        accessOperatorRepository.save(inactiveFinalOperator);
        try {
            mockMvc.perform(post("/api/scenarios/" + inactiveFinalPlanId + "/approve")
                    .session(inactiveFinalApprover)
                    .contentType(APPLICATION_JSON)
                    .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final.inactive\",\"approvalNote\":\"Inactive final owner\"}"))
                .andExpect(status().isForbidden());
        } finally {
            inactiveFinalOperator.setActive(true);
            accessOperatorRepository.save(inactiveFinalOperator);
        }

        long requesterSeparationPlanId = saveEscalatedScenarioPlan(
            tenantAdmin,
            "Phase 6 requester final separation",
            "boundary.review",
            "boundary.final",
            warehouseA
        );
        ScenarioRun requesterSeparationPlan = scenarioRunRepository.findById(requesterSeparationPlanId).orElseThrow();
        requesterSeparationPlan.setRequestedBy("boundary.final");
        requesterSeparationPlan.setApprovalStage(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);
        requesterSeparationPlan.setReviewApprovedBy("boundary.review");
        requesterSeparationPlan.setReviewApprovedAt(Instant.now());
        scenarioRunRepository.save(requesterSeparationPlan);
        mockMvc.perform(post("/api/scenarios/" + requesterSeparationPlanId + "/approve")
                .session(northFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final\",\"approvalNote\":\"Requester separation\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("different from the requester")));

        long reviewerSeparationPlanId = saveEscalatedScenarioPlan(
            tenantAdmin,
            "Phase 6 reviewer final separation",
            "boundary.review",
            "boundary.final",
            warehouseA
        );
        ScenarioRun reviewerSeparationPlan = scenarioRunRepository.findById(reviewerSeparationPlanId).orElseThrow();
        reviewerSeparationPlan.setApprovalStage(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);
        reviewerSeparationPlan.setReviewApprovedBy("boundary.final");
        reviewerSeparationPlan.setReviewApprovedAt(Instant.now());
        scenarioRunRepository.save(reviewerSeparationPlan);
        mockMvc.perform(post("/api/scenarios/" + reviewerSeparationPlanId + "/approve")
                .session(northFinalApprover)
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final\",\"approvalNote\":\"Reviewer separation\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("different from the owner reviewer")));

        mockMvc.perform(post("/api/scenarios/" + northPlanId + "/approve")
                .session(new MockHttpSession())
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"FINAL_APPROVER\",\"approverName\":\"boundary.final\",\"approvalNote\":\"Anonymous\"}"))
            .andExpect(status().isForbidden());

        var phaseEvents = businessEventRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(REHEARSAL_TENANT).stream()
            .filter(event -> event.getCreatedAt().isAfter(phaseStart))
            .toList();
        assertThat(phaseEvents).extracting(event -> event.getEventType())
            .contains(BusinessEventType.SCENARIO_ESCALATION_ADVANCED, BusinessEventType.SCENARIO_APPROVED)
            .doesNotContain(BusinessEventType.SCENARIO_EXECUTED);
        assertThat(phaseEvents.stream()
            .filter(event -> event.getEventType() == BusinessEventType.SCENARIO_APPROVED)
            .map(event -> event.getPayloadSummary())
            .anyMatch(summary -> summary.contains("boundary.final") && summary.contains("boundary.review")))
            .isTrue();

        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(REHEARSAL_TENANT)).isEqualTo(startingOrders);
        assertThat(inventoryRepository.countByTenantCode(REHEARSAL_TENANT)).isEqualTo(startingInventory);
        assertThat(fulfillmentTaskRepository.count()).isEqualTo(startingFulfillment);
        assertThat(operationalDispatchWorkItemRepository.count()).isEqualTo(startingDispatch);
        assertThat(alertRepository.count()).isEqualTo(startingAlerts);
        assertThat(recommendationRepository.count()).isEqualTo(startingRecommendations);
    }

    @Test
    void scenarioPhaseSevenSlaEscalationRequiresAssignedOwnerAndPreservesOperationalTruth() throws Exception {
        Instant phaseStart = Instant.now();
        String primaryWarehouse = warehouseA;
        long startingOrders = customerOrderRepository.countByTenant_CodeIgnoreCase(REHEARSAL_TENANT);
        long startingInventory = inventoryRepository.countByTenantCode(REHEARSAL_TENANT);
        long startingFulfillment = fulfillmentTaskRepository.count();
        long startingDispatch = operationalDispatchWorkItemRepository.count();
        long startingAlerts = alertRepository.count();
        long startingRecommendations = recommendationRepository.count();

        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD);
        long planId = savePendingFinalSlaPlan(
            tenantAdmin,
            "Phase 7 North overdue final approval",
            "boundary.review",
            "boundary.final",
            primaryWarehouse,
            Instant.now().minusMillis(1)
        );

        mockMvc.perform(get("/api/scenarios/history")
                .session(tenantAdmin)
                .param("approvalStage", "PENDING_FINAL_APPROVAL")
                .param("limit", "40"))
            .andExpect(status().isOk());

        ScenarioRun escalated = scenarioRunRepository.findById(planId).orElseThrow();
        assertThat(escalated.getApprovalPolicy()).isEqualTo(ScenarioApprovalPolicy.ESCALATED);
        assertThat(escalated.getApprovalStage()).isEqualTo(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);
        assertThat(escalated.getApprovalStatus()).isEqualTo(ScenarioApprovalStatus.PENDING_APPROVAL);
        assertThat(escalated.getSlaEscalatedAt()).isNotNull();
        assertThat(escalated.getSlaEscalatedTo()).isEqualTo("boundary.escalation");
        assertThat(escalated.getFinalApprovalOwner()).isNotBlank();
        assertThat(escalated.getApprovedBy()).isNull();

        MockHttpSession alternateEscalationOwner = tenantLogin(REHEARSAL_TENANT, "boundary.escalation.alt", ROLE_PASSWORD);
        mockMvc.perform(post("/api/scenarios/" + planId + "/acknowledge-escalation")
                .session(alternateEscalationOwner)
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation.alt", "Alternate owner")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned escalation owner")));

        MockHttpSession wrongWarehouseOwner = tenantLogin(REHEARSAL_TENANT, "boundary.escalation.b", ROLE_PASSWORD);
        mockMvc.perform(post("/api/scenarios/" + planId + "/acknowledge-escalation")
                .session(wrongWarehouseOwner)
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation.b", "Wrong warehouse")))
            .andExpect(status().isForbidden());

        assertEscalationAcknowledgementDenied(tenantAdmin, planId, "TENANT_ADMIN", "boundary.tenant.admin");
        assertEscalationAcknowledgementDenied(
            tenantLogin(REHEARSAL_TENANT, "boundary.review", ROLE_PASSWORD),
            planId,
            "REVIEW_OWNER",
            "boundary.review"
        );
        assertEscalationAcknowledgementDenied(
            tenantLogin(REHEARSAL_TENANT, "boundary.final", ROLE_PASSWORD),
            planId,
            "FINAL_APPROVER",
            "boundary.final"
        );
        assertEscalationAcknowledgementDenied(
            tenantLogin(REHEARSAL_TENANT, "boundary.integration.admin", ROLE_PASSWORD),
            planId,
            "INTEGRATION_ADMIN",
            "boundary.integration.admin"
        );
        assertEscalationAcknowledgementDenied(
            tenantLogin(REHEARSAL_TENANT, "boundary.integration.operator", ROLE_PASSWORD),
            planId,
            "INTEGRATION_OPERATOR",
            "boundary.integration.operator"
        );
        mockMvc.perform(post("/api/scenarios/" + planId + "/acknowledge-escalation")
                .session(tenantLogin(REHEARSAL_TENANT, "boundary.escalation", ROLE_PASSWORD))
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation.alt", "Actor spoof")))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/scenarios/" + planId + "/acknowledge-escalation")
                .session(new MockHttpSession())
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation", "Anonymous")))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/scenarios/" + planId + "/approve")
                .session(tenantLogin(REHEARSAL_TENANT, "boundary.escalation", ROLE_PASSWORD))
                .contentType(APPLICATION_JSON)
                .content("{\"actorRole\":\"ESCALATION_OWNER\",\"approverName\":\"boundary.escalation\",\"approvalNote\":\"Must not review or approve\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requires actor role FINAL_APPROVER")));

        long escalationEventsBeforeAcknowledgement = businessEventRepository
            .findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(REHEARSAL_TENANT).stream()
            .filter(event -> event.getCreatedAt().isAfter(phaseStart))
            .filter(event -> event.getEventType() == BusinessEventType.SCENARIO_SLA_ESCALATED)
            .filter(event -> event.getPayloadSummary().contains("Phase 7 North overdue final approval"))
            .count();
        assertThat(escalationEventsBeforeAcknowledgement).isEqualTo(1);

        MockHttpSession assignedEscalationOwner = tenantLogin(REHEARSAL_TENANT, "boundary.escalation", ROLE_PASSWORD);
        mockMvc.perform(post("/api/scenarios/" + planId + "/acknowledge-escalation")
                .session(assignedEscalationOwner)
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation", "Assigned owner took follow-up.")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalPolicy").value("ESCALATED"))
            .andExpect(jsonPath("$.approvalStage").value("PENDING_FINAL_APPROVAL"))
            .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL"))
            .andExpect(jsonPath("$.slaAcknowledged").value(true))
            .andExpect(jsonPath("$.slaAcknowledgedBy").value("boundary.escalation"))
            .andExpect(jsonPath("$.slaAcknowledgementNote").value("Assigned owner took follow-up."))
            .andExpect(jsonPath("$.approvedBy").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(post("/api/scenarios/" + planId + "/acknowledge-escalation")
                .session(assignedEscalationOwner)
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation", "A safe duplicate retry.")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slaAcknowledgedBy").value("boundary.escalation"));

        long acknowledgementEventsAfterRetry = businessEventRepository
            .findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(REHEARSAL_TENANT).stream()
            .filter(event -> event.getCreatedAt().isAfter(phaseStart))
            .filter(event -> event.getEventType() == BusinessEventType.SCENARIO_SLA_ACKNOWLEDGED)
            .filter(event -> event.getPayloadSummary().contains("Phase 7 North overdue final approval"))
            .count();
        assertThat(acknowledgementEventsAfterRetry).isEqualTo(1);

        ScenarioRun acknowledged = scenarioRunRepository.findById(planId).orElseThrow();
        assertThat(acknowledged.getApprovalStage()).isEqualTo(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);
        assertThat(acknowledged.getApprovalStatus()).isEqualTo(ScenarioApprovalStatus.PENDING_APPROVAL);
        assertThat(acknowledged.getApprovedBy()).isNull();
        assertThat(customerOrderRepository.countByTenant_CodeIgnoreCase(REHEARSAL_TENANT)).isEqualTo(startingOrders);
        assertThat(inventoryRepository.countByTenantCode(REHEARSAL_TENANT)).isEqualTo(startingInventory);
        assertThat(fulfillmentTaskRepository.count()).isEqualTo(startingFulfillment);
        assertThat(operationalDispatchWorkItemRepository.count()).isEqualTo(startingDispatch);
        assertThat(alertRepository.count()).isEqualTo(startingAlerts);
        assertThat(recommendationRepository.count()).isEqualTo(startingRecommendations);
    }

    @Test
    void scenarioPhaseSevenSlaEscalationSeparatesCoastTenantAndWorkflowBoundaries() throws Exception {
        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD);
        long coastPlanId = savePendingFinalSlaPlan(
            tenantAdmin,
            "Phase 7 secondary warehouse overdue final approval",
            "boundary.review.b",
            "boundary.final.b",
            warehouseB,
            Instant.now().minusSeconds(1)
        );
        mockMvc.perform(get("/api/scenarios/history").session(tenantAdmin).param("limit", "40"))
            .andExpect(status().isOk());

        ScenarioRun coastEscalated = scenarioRunRepository.findById(coastPlanId).orElseThrow();
        assertThat(coastEscalated.getSlaEscalatedAt()).isNotNull();
        assertThat(coastEscalated.getSlaEscalatedTo()).isEqualTo("boundary.escalation.b");
        assertThat(coastEscalated.getApprovalStage()).isEqualTo(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);

        mockMvc.perform(post("/api/scenarios/" + coastPlanId + "/acknowledge-escalation")
                .session(tenantLogin(REHEARSAL_TENANT, "boundary.escalation", ROLE_PASSWORD))
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation", "North owner on Coast")))
            .andExpect(status().isForbidden());

        assertEscalationAcknowledgementDenied(
            tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD),
            coastPlanId,
            "TENANT_ADMIN",
            "boundary.tenant.admin"
        );
        assertEscalationAcknowledgementDenied(
            tenantLogin(REHEARSAL_TENANT, "boundary.review.b", ROLE_PASSWORD),
            coastPlanId,
            "REVIEW_OWNER",
            "boundary.review.b"
        );
        assertEscalationAcknowledgementDenied(
            tenantLogin(REHEARSAL_TENANT, "boundary.final.b", ROLE_PASSWORD),
            coastPlanId,
            "FINAL_APPROVER",
            "boundary.final.b"
        );
        assertEscalationAcknowledgementDenied(
            tenantLogin(REHEARSAL_TENANT, "boundary.integration.admin.b", ROLE_PASSWORD),
            coastPlanId,
            "INTEGRATION_ADMIN",
            "boundary.integration.admin.b"
        );
        assertEscalationAcknowledgementDenied(
            tenantLogin(REHEARSAL_TENANT, "boundary.requester.b", ROLE_PASSWORD),
            coastPlanId,
            "INTEGRATION_OPERATOR",
            "boundary.requester.b"
        );

        mockMvc.perform(post("/api/scenarios/" + coastPlanId + "/acknowledge-escalation")
                .session(new MockHttpSession())
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation.b", "Anonymous Coast")))
            .andExpect(status().isForbidden());

        MockHttpSession spoofSession = tenantLogin(REHEARSAL_TENANT, "boundary.escalation.b", ROLE_PASSWORD);
        mockMvc.perform(post("/api/scenarios/" + coastPlanId + "/acknowledge-escalation")
                .session(spoofSession)
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation", "Spoofed actor")))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/scenarios/" + coastPlanId + "/acknowledge-escalation")
                .session(tenantLogin(REHEARSAL_TENANT, "boundary.escalation.b", ROLE_PASSWORD))
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation.b", "Coast owner took follow-up.")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slaAcknowledgedBy").value("boundary.escalation.b"))
            .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL"));

        long crossTenantScenarioId = coastPlanId;
        mockMvc.perform(post("/api/scenarios/" + crossTenantScenarioId + "/acknowledge-escalation")
                .session(tenantLogin(ISOLATION_TENANT, "isolation.admin", "Isolation-Admin-2026!"))
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation.b", "Cross tenant")))
            .andExpect(status().isNotFound());

        long nonOverduePlanId = savePendingFinalSlaPlan(
            tenantAdmin,
            "Phase 7 non-overdue plan",
            "boundary.review.b",
            "boundary.final.b",
            warehouseB,
            Instant.now().plusSeconds(3600)
        );
        mockMvc.perform(post("/api/scenarios/" + nonOverduePlanId + "/acknowledge-escalation")
                .session(tenantLogin(REHEARSAL_TENANT, "boundary.escalation.b", ROLE_PASSWORD))
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation.b", "Not overdue")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not been SLA escalated")));

        long previewId = createPreviewScenario(tenantAdmin, warehouseB);
        mockMvc.perform(post("/api/scenarios/" + previewId + "/acknowledge-escalation")
                .session(tenantLogin(REHEARSAL_TENANT, "boundary.escalation.b", ROLE_PASSWORD))
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation.b", "Preview")))
            .andExpect(status().isBadRequest());

        long approvedPlanId = saveStandardScenarioPlan(tenantAdmin, "Phase 7 approved state", "boundary.review.b", warehouseB);
        ScenarioRun approvedPlan = scenarioRunRepository.findById(approvedPlanId).orElseThrow();
        approvedPlan.setApprovalStatus(ScenarioApprovalStatus.APPROVED);
        approvedPlan.setApprovalStage(ScenarioApprovalStage.APPROVED);
        approvedPlan.setApprovalDueAt(null);
        scenarioRunRepository.saveAndFlush(approvedPlan);
        mockMvc.perform(post("/api/scenarios/" + approvedPlanId + "/acknowledge-escalation")
                .session(tenantLogin(REHEARSAL_TENANT, "boundary.escalation.b", ROLE_PASSWORD))
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation.b", "Approved state")))
            .andExpect(status().isBadRequest());

        long rejectedPlanId = savePendingFinalSlaPlan(
            tenantAdmin,
            "Phase 7 rejected state",
            "boundary.review.b",
            "boundary.final.b",
            warehouseB,
            Instant.now().minusSeconds(1)
        );
        ScenarioRun rejectedPlan = scenarioRunRepository.findById(rejectedPlanId).orElseThrow();
        rejectedPlan.setApprovalStatus(ScenarioApprovalStatus.REJECTED);
        rejectedPlan.setApprovalStage(ScenarioApprovalStage.REJECTED);
        rejectedPlan.setSlaEscalatedAt(Instant.now());
        rejectedPlan.setSlaEscalatedTo("boundary.escalation.b");
        scenarioRunRepository.saveAndFlush(rejectedPlan);
        mockMvc.perform(post("/api/scenarios/" + rejectedPlanId + "/acknowledge-escalation")
                .session(tenantLogin(REHEARSAL_TENANT, "boundary.escalation.b", ROLE_PASSWORD))
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation.b", "Rejected state")))
            .andExpect(status().isBadRequest());

        long missingOwnerPlanId = savePendingFinalSlaPlan(
            tenantAdmin,
            "Phase 7 missing escalation owner",
            "boundary.review.b",
            "boundary.final.b",
            warehouseB,
            Instant.now().minusSeconds(1)
        );
        ScenarioRun missingOwnerPlan = scenarioRunRepository.findById(missingOwnerPlanId).orElseThrow();
        missingOwnerPlan.setSlaEscalatedAt(Instant.now());
        missingOwnerPlan.setSlaEscalatedTo(null);
        scenarioRunRepository.saveAndFlush(missingOwnerPlan);
        mockMvc.perform(post("/api/scenarios/" + missingOwnerPlanId + "/acknowledge-escalation")
                .session(tenantLogin(REHEARSAL_TENANT, "boundary.escalation.b", ROLE_PASSWORD))
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation.b", "Missing assignment")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned escalation owner")));

        long missingNotePlanId = savePendingFinalSlaPlan(
            tenantAdmin,
            "Phase 7 missing acknowledgement note",
            "boundary.review.b",
            "boundary.final.b",
            warehouseB,
            Instant.now().minusSeconds(1)
        );
        ScenarioRun missingNotePlan = scenarioRunRepository.findById(missingNotePlanId).orElseThrow();
        missingNotePlan.setSlaEscalatedAt(Instant.now());
        missingNotePlan.setSlaEscalatedTo("boundary.escalation.b");
        scenarioRunRepository.saveAndFlush(missingNotePlan);
        mockMvc.perform(post("/api/scenarios/" + missingNotePlanId + "/acknowledge-escalation")
                .session(tenantLogin(REHEARSAL_TENANT, "boundary.escalation.b", ROLE_PASSWORD))
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload("ESCALATION_OWNER", "boundary.escalation.b", "   ")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void scenarioPhaseSevenSlaDeadlineSeparatesReviewStageAndKeepsHistoryTruthful() throws Exception {
        MockHttpSession tenantAdmin = tenantLogin(REHEARSAL_TENANT, "boundary.tenant.admin", ROLE_PASSWORD);
        long reviewStagePlanId = saveEscalatedScenarioPlan(
            tenantAdmin,
            "Phase 7 overdue review stage remains review-owned",
            "boundary.review",
            "boundary.final",
            warehouseA
        );
        ScenarioRun reviewStagePlan = scenarioRunRepository.findById(reviewStagePlanId).orElseThrow();
        reviewStagePlan.setApprovalStage(ScenarioApprovalStage.PENDING_REVIEW);
        reviewStagePlan.setApprovalStatus(ScenarioApprovalStatus.PENDING_APPROVAL);
        reviewStagePlan.setApprovalDueAt(Instant.now().minusMillis(1));
        reviewStagePlan.setSlaEscalatedAt(null);
        reviewStagePlan.setSlaEscalatedTo(null);
        scenarioRunRepository.saveAndFlush(reviewStagePlan);

        mockMvc.perform(get("/api/scenarios/history")
                .session(tenantAdmin)
                .param("approvalStage", "PENDING_REVIEW")
                .param("overdueOnly", "true")
                .param("limit", "40"))
            .andExpect(status().isOk());

        ScenarioRun reviewStageReadback = scenarioRunRepository.findById(reviewStagePlanId).orElseThrow();
        assertThat(reviewStageReadback.getApprovalStage()).isEqualTo(ScenarioApprovalStage.PENDING_REVIEW);
        assertThat(reviewStageReadback.getApprovalStatus()).isEqualTo(ScenarioApprovalStatus.PENDING_APPROVAL);
        assertThat(reviewStageReadback.getSlaEscalatedAt()).isNull();
        assertThat(reviewStageReadback.getSlaEscalatedTo()).isNull();
        assertThat(reviewStageReadback.getApprovalDueAt()).isBefore(Instant.now());

        long exactDeadlinePlanId = savePendingFinalSlaPlan(
            tenantAdmin,
            "Phase 7 exact deadline boundary",
            "boundary.review",
            "boundary.final",
            warehouseA,
            Instant.now()
        );
        mockMvc.perform(get("/api/scenarios/history")
                .session(tenantAdmin)
                .param("approvalStage", "PENDING_FINAL_APPROVAL")
                .param("overdueOnly", "true")
                .param("limit", "40"))
            .andExpect(status().isOk());
        ScenarioRun exactDeadlineReadback = scenarioRunRepository.findById(exactDeadlinePlanId).orElseThrow();
        assertThat(exactDeadlineReadback.getSlaEscalatedAt()).isNotNull();
        assertThat(exactDeadlineReadback.getSlaEscalatedTo()).isEqualTo("boundary.escalation");
        assertThat(exactDeadlineReadback.getApprovalStage()).isEqualTo(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);
        assertThat(exactDeadlineReadback.getApprovalStatus()).isEqualTo(ScenarioApprovalStatus.PENDING_APPROVAL);

        var historyEvents = businessEventRepository.findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(REHEARSAL_TENANT);
        assertThat(historyEvents).extracting(event -> event.getEventType())
            .contains(BusinessEventType.SCENARIO_SLA_ESCALATED);
        assertThat(historyEvents.stream()
            .filter(event -> event.getEventType() == BusinessEventType.SCENARIO_SLA_ESCALATED)
            .map(event -> event.getPayloadSummary())
            .anyMatch(summary -> summary.contains("Phase 7 exact deadline boundary") && summary.contains("boundary.escalation")))
            .isTrue();
    }

    private void assertEscalationAcknowledgementDenied(MockHttpSession session,
                                                       long scenarioId,
                                                       String actorRole,
                                                       String acknowledgedBy) throws Exception {
        mockMvc.perform(post("/api/scenarios/" + scenarioId + "/acknowledge-escalation")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content(acknowledgementPayload(actorRole, acknowledgedBy, "Expected denial")))
            .andExpect(status().isBadRequest());
    }

    private String acknowledgementPayload(String actorRole, String acknowledgedBy, String note) {
        return "{\"actorRole\":\"%s\",\"acknowledgedBy\":\"%s\",\"note\":\"%s\"}"
            .formatted(actorRole, acknowledgedBy, note);
    }

    private long savePendingFinalSlaPlan(MockHttpSession session,
                                         String title,
                                         String reviewOwner,
                                         String finalApprovalOwner,
                                         String warehouseCode,
                                         Instant dueAt) throws Exception {
        long planId = saveEscalatedScenarioPlan(session, title, reviewOwner, finalApprovalOwner, warehouseCode);
        ScenarioRun plan = scenarioRunRepository.findById(planId).orElseThrow();
        plan.setApprovalStage(ScenarioApprovalStage.PENDING_FINAL_APPROVAL);
        plan.setApprovalStatus(ScenarioApprovalStatus.PENDING_APPROVAL);
        plan.setReviewApprovedBy(reviewOwner);
        plan.setReviewApprovedAt(Instant.now().minusSeconds(30));
        plan.setApprovalDueAt(dueAt);
        plan.setSlaEscalatedTo(null);
        plan.setSlaEscalatedAt(null);
        plan.setSlaAcknowledgedBy(null);
        plan.setSlaAcknowledgedAt(null);
        plan.setSlaAcknowledgementNote(null);
        scenarioRunRepository.saveAndFlush(plan);
        return planId;
    }

    private void assertRequesterSpoofRejected(MockHttpSession session,
                                              String title,
                                              String requestedBy,
                                              String warehouseCode) throws Exception {
        mockMvc.perform(post("/api/scenarios/save")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content(scenarioSavePayload(title, requestedBy, "boundary.review", warehouseCode)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                "authenticated session actor")));
    }

    private String scenarioSavePayload(String title,
                                       String requestedBy,
                                       String reviewOwner,
                                       String warehouseCode) {
        return scenarioSavePayload(title, requestedBy, reviewOwner, warehouseCode, 1);
    }

    private String scenarioSavePayload(String title,
                                       String requestedBy,
                                       String reviewOwner,
                                       String warehouseCode,
                                       int quantity) {
        return """
            {"title":"%s","requestedBy":"%s","reviewOwner":"%s","request":{"warehouseCode":"%s","items":[{"productSku":"BOUNDARY-SKU","quantity":%d,"unitPrice":10.00}]}}
            """.formatted(title, requestedBy, reviewOwner, warehouseCode, quantity);
    }

    private String scenarioComparePayload(String warehouseCode) {
        return scenarioComparePayload(warehouseCode, warehouseCode);
    }

    private String scenarioComparePayload(String primaryWarehouseCode, String alternativeWarehouseCode) {
        return """
            {
              "primaryLabel":"Phase 1 Primary",
              "primary":{"warehouseCode":"%s","items":[{"productSku":"BOUNDARY-SKU","quantity":1,"unitPrice":10.00}]},
              "alternativeLabel":"Phase 1 Alternative",
              "alternative":{"warehouseCode":"%s","items":[{"productSku":"BOUNDARY-SKU","quantity":2,"unitPrice":10.00}]}
            }
            """.formatted(primaryWarehouseCode, alternativeWarehouseCode);
    }

    private void createRoleUser(MockHttpSession admin,
                                String identity,
                                String role,
                                List<String> warehouseScopes) throws Exception {
        String scopesJson = warehouseScopes.stream().map(scope -> "\"" + scope + "\"")
            .collect(java.util.stream.Collectors.joining(","));
        mockMvc.perform(post("/api/access/admin/operators")
                .session(admin)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"actorName":"%s","displayName":"%s","description":"Synthetic boundary rehearsal role","active":true,"roles":["%s"],"warehouseScopes":[%s]}
                    """.formatted(identity, identity, role, scopesJson)))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/access/admin/users")
                .session(admin)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"username":"%s","fullName":"%s","password":"%s","operatorActorName":"%s"}
                    """.formatted(identity, identity, ROLE_PASSWORD, identity)))
            .andExpect(status().isOk());
    }

    private MockHttpSession tenantLogin(String tenantCode, String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content(loginPayload(tenantCode, username, password)))
            .andExpect(status().isOk())
            .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private MockHttpSession platformLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/platform/session/login")
                .contentType(APPLICATION_JSON)
                .content("{\"username\":\"platform.owner\",\"password\":\"Boundary-Platform-2026!\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void updateInventory(MockHttpSession session, String warehouseCode, long quantity) throws Exception {
        mockMvc.perform(post("/api/inventory/update")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content(inventoryPayload(warehouseCode, quantity)))
            .andExpect(status().isOk());
    }

    private long saveStandardScenarioPlan(MockHttpSession session,
                                          String title,
                                          String reviewOwner,
                                          String warehouseCode) throws Exception {
        mockMvc.perform(post("/api/scenarios/save")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"title":"%s","requestedBy":"boundary.tenant.admin","reviewOwner":"%s","request":{"warehouseCode":"%s","items":[{"productSku":"BOUNDARY-SKU","quantity":1,"unitPrice":10.00}]}}
                    """.formatted(title, reviewOwner, warehouseCode)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value(title))
            .andExpect(jsonPath("$.reviewOwner").value(reviewOwner));
        return scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.SAVED_PLAN)
            .filter(run -> title.equals(run.getTitle()))
            .findFirst()
            .orElseThrow()
            .getId();
    }

    private long saveEscalatedScenarioPlan(MockHttpSession session,
                                           String title,
                                           String reviewOwner,
                                           String finalApprovalOwner,
                                           String warehouseCode) throws Exception {
        long planId = saveStandardScenarioPlan(session, title, reviewOwner, warehouseCode);
        ScenarioRun plan = scenarioRunRepository.findById(planId).orElseThrow();
        plan.setApprovalPolicy(ScenarioApprovalPolicy.ESCALATED);
        plan.setReviewPriority(ScenarioReviewPriority.HIGH);
        plan.setApprovalStage(ScenarioApprovalStage.PENDING_REVIEW);
        plan.setApprovalStatus(ScenarioApprovalStatus.PENDING_APPROVAL);
        plan.setFinalApprovalOwner(finalApprovalOwner);
        plan.setApprovalDueAt(Instant.now().plusSeconds(3600));
        scenarioRunRepository.save(plan);
        return planId;
    }

    private long createPreviewScenario(MockHttpSession session, String warehouseCode) throws Exception {
        mockMvc.perform(post("/api/scenarios/order-impact")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content(orderPayload(null, warehouseCode)))
            .andExpect(status().isOk());
        return scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.PREVIEW)
            .filter(run -> warehouseCode.equalsIgnoreCase(run.getWarehouseCode()))
            .findFirst()
            .orElseThrow()
            .getId();
    }

    private String tenantPayload(String tenantCode, String tenantName, String username, String password) {
        return """
            {"tenantCode":"%s","tenantName":"%s","description":"Synthetic access-boundary rehearsal.","adminFullName":"Boundary Administrator","adminUsername":"%s","adminPassword":"%s","primaryLocation":"Boundary Warehouse A","secondaryLocation":"Boundary Warehouse B"}
            """.formatted(tenantCode, tenantName, username, password);
    }

    private String loginPayload(String tenantCode, String username, String password) {
        return "{\"tenantCode\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"}"
            .formatted(tenantCode, username, password);
    }

    private String inventoryPayload(String warehouseCode, long quantity) {
        return "{\"productSku\":\"BOUNDARY-SKU\",\"warehouseCode\":\"%s\",\"quantityAvailable\":%d,\"reorderThreshold\":5}"
            .formatted(warehouseCode, quantity);
    }

    private String connectorPayload(String sourceSystem) {
        return """
            {
              "sourceSystem":"%s",
              "type":"CSV_ORDER_IMPORT",
              "displayName":"Boundary Role Connector",
              "enabled":false,
              "syncMode":"BATCH_FILE_DROP",
              "validationPolicy":"RELAXED",
              "transformationPolicy":"NORMALIZE_CODES",
              "allowDefaultWarehouseFallback":false,
              "defaultWarehouseCode":"%s",
              "notes":"Synthetic role boundary verification."
            }
            """.formatted(sourceSystem, warehouseA);
    }

    private String enabledConnectorPayload(String sourceSystem, String type) {
        return """
            {
              "sourceSystem":"%s",
              "type":"%s",
              "displayName":"Boundary Ingestion Connector",
              "enabled":true,
              "syncMode":"BATCH_FILE_DROP",
              "validationPolicy":"RELAXED",
              "transformationPolicy":"NORMALIZE_CODES",
              "allowDefaultWarehouseFallback":false,
              "defaultWarehouseCode":"%s",
              "notes":"Synthetic ingestion authority verification."
            }
            """.formatted(sourceSystem, type, warehouseA);
    }

    private String webhookPayload(String sourceSystem, String externalOrderId) {
        return """
            {
              "sourceSystem":"%s",
              "externalOrderId":"%s",
              "warehouseCode":"%s",
              "customerReference":"BOUNDARY-CUSTOMER",
              "occurredAt":"2026-08-22T10:00:00Z",
              "items":[{"productSku":"BOUNDARY-SKU","quantity":1,"unitPrice":10.00}]
            }
            """.formatted(sourceSystem, externalOrderId, warehouseA);
    }

    private MockMultipartFile csvFile(String externalOrderId, String sourceSystem) {
        String csv = "sourceSystem,externalOrderId,warehouseCode,productSku,quantity,unitPrice\n"
            + sourceSystem + "," + externalOrderId + "," + warehouseA + ",BOUNDARY-SKU,1,10.00\n";
        return new MockMultipartFile(
            "file",
            "orders.csv",
            "text/csv",
            csv.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String orderPayload(String externalOrderId, String warehouseCode) {
        String externalOrder = externalOrderId == null ? "null" : "\"" + externalOrderId + "\"";
        return "{\"externalOrderId\":" + externalOrder
            + ",\"warehouseCode\":\"" + warehouseCode
            + "\",\"items\":[{\"productSku\":\"BOUNDARY-SKU\",\"quantity\":1,\"unitPrice\":10.00}]}";
    }

    private record RoleExpectation(String username, String role, boolean tenantWide) {
    }

    private record ScenarioCreationExpectation(String username, String reviewOwner, String warehouseCode) {
    }
}
