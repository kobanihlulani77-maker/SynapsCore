package com.synapsecore;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synapsecore.access.BootstrapAccessService;
import com.synapsecore.access.PlatformAdministrationAccessService;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.domain.repository.ScenarioRunRepository;
import com.synapsecore.domain.entity.ScenarioRunType;
import com.synapsecore.config.SynapsePlatformOwnerProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
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
        createRoleUser(admin, "boundary.review", "REVIEW_OWNER", List.of(warehouseA));
        createRoleUser(admin, "boundary.final", "FINAL_APPROVER", List.of(warehouseA));
        createRoleUser(admin, "boundary.escalation", "ESCALATION_OWNER", List.of(warehouseA));
        createRoleUser(admin, "boundary.integration.admin", "INTEGRATION_ADMIN", List.of(warehouseA));
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
        mockMvc.perform(post("/api/orders")
                .session(admin)
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
    void roleSessionsCannotReachPlatformOrUnrelatedAdministrativeActions() throws Exception {
        for (String username : List.of(
            "boundary.review",
            "boundary.final",
            "boundary.escalation",
            "boundary.integration.admin",
            "boundary.integration.operator"
        )) {
            MockHttpSession session = tenantLogin(REHEARSAL_TENANT, username, ROLE_PASSWORD);
            mockMvc.perform(get("/api/platform/overview").session(session))
                .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/access/admin/users").session(session))
                .andExpect(status().isForbidden());
        }
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
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("payloadSummary"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("details"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("targetRef"))));
        mockMvc.perform(get("/api/platform/runtime").session(platformSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeProfiles").exists())
            .andExpect(jsonPath("$.build.commit").exists())
            .andExpect(jsonPath("$.allowedOrigins").doesNotExist());
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

    private String orderPayload(String externalOrderId, String warehouseCode) {
        String externalOrder = externalOrderId == null ? "null" : "\"" + externalOrderId + "\"";
        return "{\"externalOrderId\":" + externalOrder
            + ",\"warehouseCode\":\"" + warehouseCode
            + "\",\"items\":[{\"productSku\":\"BOUNDARY-SKU\",\"quantity\":1,\"unitPrice\":10.00}]}";
    }
}
