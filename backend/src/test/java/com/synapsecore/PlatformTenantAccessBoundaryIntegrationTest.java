package com.synapsecore;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synapsecore.access.BootstrapAccessService;
import com.synapsecore.access.PlatformAdministrationAccessService;
import com.synapsecore.domain.entity.ScenarioApprovalPolicy;
import com.synapsecore.domain.entity.ScenarioApprovalStage;
import com.synapsecore.domain.entity.ScenarioApprovalStatus;
import com.synapsecore.domain.entity.ScenarioReviewPriority;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.AccessUserRepository;
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
        createRoleUser(admin, "boundary.final", "FINAL_APPROVER", List.of(warehouseA));
        createRoleUser(admin, "boundary.final.alt", "FINAL_APPROVER", List.of(warehouseA));
        createRoleUser(admin, "boundary.escalation", "ESCALATION_OWNER", List.of(warehouseA));
        createRoleUser(admin, "boundary.escalation.alt", "ESCALATION_OWNER", List.of(warehouseA));
        createRoleUser(admin, "boundary.integration.admin", "INTEGRATION_ADMIN", List.of(warehouseA));
        createRoleUser(admin, "boundary.integration.admin.all", "INTEGRATION_ADMIN", List.of());
        createRoleUser(admin, "boundary.integration.operator", "INTEGRATION_OPERATOR", List.of(warehouseA));

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

        long warehouseBScenarioId = scenarioRunRepository.findTop12ByOrderByCreatedAtDesc().stream()
            .filter(run -> run.getType() == ScenarioRunType.PREVIEW)
            .filter(run -> warehouseB.equalsIgnoreCase(run.getWarehouseCode()))
            .findFirst()
            .orElseThrow()
            .getId();
        mockMvc.perform(get("/api/scenarios/history").session(scopedOperator))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("\"id\":" + warehouseBScenarioId))));
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
}
