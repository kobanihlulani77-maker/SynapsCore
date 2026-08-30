package com.synapsecore;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synapsecore.access.SynapseAccessRole;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.AccessUser;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.AccessUserRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.profiles.active=prod",
    "spring.datasource.url=jdbc:h2:mem:domain13phase1;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.session.store-type=none",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.session.SessionAutoConfiguration",
    "spring.data.redis.url=redis://localhost:6379",
    "management.health.redis.enabled=false",
    "management.endpoint.health.group.readiness.include=readinessState,db,ping",
    "synapsecore.realtime.broker-mode=SIMPLE_IN_MEMORY",
    "synapsecore.access.allow-header-fallback=false",
    "synapsecore.starter.auto-seed-on-empty=false",
    "synapsecore.starter.allow-default-tenant-fallback=false",
    "synapsecore.security.rate-limit.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class TenantWorkspaceAdminPhase1IntegrationTest {

    private static final String ADMIN_USERNAME = "phase1.admin";
    private static final String ADMIN_PASSWORD = "Phase1-Admin-2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private AccessOperatorRepository accessOperatorRepository;

    @Autowired
    private AccessUserRepository accessUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() throws Exception {
        String code = "D13-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        tenant = tenantRepository.save(Tenant.builder()
            .code(code)
            .name("Domain 13 Phase 1")
            .requiredRoles(EnumSet.of(SynapseAccessRole.REVIEW_OWNER))
            .build());
        warehouseRepository.save(Warehouse.builder()
            .tenant(tenant).code("WH-NORTH").name("North").location("North").build());
        warehouseRepository.save(Warehouse.builder()
            .tenant(tenant).code("WH-COAST").name("Coast").location("Coast").build());
        AccessOperator adminOperator = accessOperatorRepository.save(AccessOperator.builder()
            .tenant(tenant)
            .actorName("Phase 1 Admin")
            .displayName("Phase 1 Admin")
            .active(true)
            .roles(EnumSet.of(SynapseAccessRole.TENANT_ADMIN))
            .warehouseScopes(Set.of())
            .build());
        accessUserRepository.save(AccessUser.builder()
            .tenant(tenant)
            .username(ADMIN_USERNAME)
            .fullName("Phase 1 Admin")
            .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
            .active(true)
            .operator(adminOperator)
            .build());
        adminSession = signIn(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test
    void operatorScopeMustBeExplicitAndRoleOnlyUpdatePreservesScope() throws Exception {
        createReviewCoverage("baseline.review", "Baseline Review Owner");

        mockMvc.perform(post("/api/access/admin/operators")
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content(operatorJson("Ambiguous Operator", "[\"REVIEW_OWNER\"]", "[]", null, null, true)))
            .andExpect(status().isBadRequest());

        MvcResult created = mockMvc.perform(post("/api/access/admin/operators")
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content(operatorJson("North Reviewer", "[\"REVIEW_OWNER\"]", "[\"WH-NORTH\"]", false, null, true)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.warehouseScopes[0]").value("WH-NORTH"))
            .andExpect(jsonPath("$.version").value(0))
            .andReturn();

        long operatorId = objectId(created);
        mockMvc.perform(put("/api/access/admin/operators/" + operatorId)
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorName":"North Reviewer",
                      "displayName":"North Reviewer",
                      "description":"Role-only update",
                      "active":true,
                      "roles":["REVIEW_OWNER","INTEGRATION_ADMIN"],
                      "version":0
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.warehouseScopes[0]").value("WH-NORTH"))
            .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void requiredReviewCoverageCannotBeRemovedFromLastUsableHolder() throws Exception {
        MvcResult operator = mockMvc.perform(post("/api/access/admin/operators")
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content(operatorJson("Review Owner", "[\"REVIEW_OWNER\"]", "[]", true, null, true)))
            .andExpect(status().isOk())
            .andReturn();
        long operatorId = objectId(operator);

        mockMvc.perform(post("/api/access/admin/users")
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "username":"review.owner",
                      "fullName":"Review Owner",
                      "password":"Review-Owner-2026!",
                      "operatorActorName":"Review Owner"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/access/admin/operators/" + operatorId)
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorName":"Review Owner",
                      "displayName":"Review Owner",
                      "active":true,
                      "roles":[],
                      "tenantWide":true,
                      "version":0
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("required REVIEW_OWNER coverage")));
    }

    @Test
    void staleOperatorUpdateReturnsConflictInsteadOfRestoringRemovedScope() throws Exception {
        createReviewCoverage("baseline.stale.review", "Baseline Stale Review Owner");

        MvcResult created = mockMvc.perform(post("/api/access/admin/operators")
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content(operatorJson("Scoped Operator", "[\"INTEGRATION_OPERATOR\"]", "[\"WH-NORTH\",\"WH-COAST\"]", false, null, true)))
            .andExpect(status().isOk())
            .andReturn();
        long operatorId = objectId(created);

        mockMvc.perform(put("/api/access/admin/operators/" + operatorId)
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content(operatorJson("Scoped Operator", "[\"INTEGRATION_OPERATOR\"]", "[\"WH-NORTH\"]", false, 0, true)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.warehouseScopes[0]").value("WH-NORTH"))
            .andExpect(jsonPath("$.warehouseScopes").isArray());

        mockMvc.perform(put("/api/access/admin/operators/" + operatorId)
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content(operatorJson("Scoped Operator", "[\"INTEGRATION_OPERATOR\",\"INTEGRATION_ADMIN\"]", "[\"WH-NORTH\",\"WH-COAST\"]", false, 0, true)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Administrative operator changed. Refresh and try again."));
    }

    @Test
    void invalidRoleAndInactiveOperatorCannotCreateUser() throws Exception {
        createReviewCoverage("baseline.inactive.review", "Baseline Inactive Review Owner");

        mockMvc.perform(post("/api/access/admin/operators")
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content(operatorJson("Invalid Role", "[\"PLATFORM_OWNER\"]", "[]", true, null, true)))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/access/admin/operators")
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content(operatorJson("Inactive Operator", "[\"INTEGRATION_OPERATOR\"]", "[\"WH-NORTH\"]", false, null, false)))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/access/admin/operators/" + accessOperatorRepository
                .findByTenant_CodeIgnoreCaseAndActorNameIgnoreCase(tenant.getCode(), "Inactive Operator").orElseThrow().getId())
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content(operatorJson("Inactive Operator", "[\"INTEGRATION_OPERATOR\"]", "[\"WH-NORTH\"]", false, 0, false)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/access/admin/users")
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "username":"inactive.operator.user",
                      "fullName":"Inactive Operator User",
                      "password":"Inactive-User-2026!",
                      "operatorActorName":"Inactive Operator"
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    private MockHttpSession signIn(String username, String password) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"tenantCode":"%s","username":"%s","password":"%s"}
                    """.formatted(tenant.getCode(), username, password)))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);
    }

    private void createReviewCoverage(String username, String actorName) throws Exception {
        mockMvc.perform(post("/api/access/admin/operators")
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content(operatorJson(actorName, "[\"REVIEW_OWNER\"]", "[]", true, null, true)))
            .andExpect(status().isOk())
            .andReturn();

        mockMvc.perform(post("/api/access/admin/users")
                .session(adminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "username":"%s",
                      "fullName":"%s",
                      "password":"Baseline-Review-2026!",
                      "operatorActorName":"%s"
                    }
                    """.formatted(username, actorName, actorName)))
            .andExpect(status().isOk());
    }

    private String operatorJson(String actorName,
                                String roles,
                                String scopes,
                                Boolean tenantWide,
                                Integer version,
                                boolean active) {
        String tenantWideJson = tenantWide == null ? "null" : tenantWide.toString();
        String versionJson = version == null ? "null" : version.toString();
        return """
            {"actorName":"%s","displayName":"%s","description":"Phase 1 fixture","active":%s,"roles":%s,"warehouseScopes":%s,"tenantWide":%s,"version":%s}
            """.formatted(actorName, actorName, active ? "true" : "false",
            roles, scopes, tenantWideJson, versionJson);
    }

    private long objectId(MvcResult result) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(result.getResponse().getContentAsString())
            .path("id")
            .asLong();
    }
}
