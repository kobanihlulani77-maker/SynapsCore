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
    "spring.datasource.url=jdbc:h2:mem:domain13phase2;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class TenantWorkspaceAdminPhase2IntegrationTest {

    private static final String ADMIN_USERNAME = "phase2.admin";
    private static final String ADMIN_PASSWORD = "Phase2-Admin-2026!";

    @Autowired private MockMvc mockMvc;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private AccessOperatorRepository accessOperatorRepository;
    @Autowired private AccessUserRepository accessUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() throws Exception {
        tenant = tenantRepository.save(Tenant.builder()
            .code("D13P2-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .name("Domain 13 Phase 2")
            .build());
        warehouseRepository.save(Warehouse.builder().tenant(tenant).code("WH-NORTH").name("North").location("North").build());
        warehouseRepository.save(Warehouse.builder().tenant(tenant).code("WH-COAST").name("Coast").location("Coast").build());
        AccessOperator operator = accessOperatorRepository.save(AccessOperator.builder()
            .tenant(tenant).actorName("Phase 2 Admin").displayName("Phase 2 Admin").active(true)
            .roles(EnumSet.of(SynapseAccessRole.TENANT_ADMIN)).warehouseScopes(Set.of()).build());
        accessUserRepository.save(AccessUser.builder().tenant(tenant).username(ADMIN_USERNAME)
            .fullName("Phase 2 Admin").passwordHash(passwordEncoder.encode(ADMIN_PASSWORD)).active(true)
            .operator(operator).build());
        adminSession = signIn();
    }

    @Test
    void workspaceAndWarehouseMutationsUseVersionsAndRetirementIsNonDestructive() throws Exception {
        MvcResult workspace = mockMvc.perform(get("/api/access/admin/workspace").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(0))
            .andExpect(jsonPath("$.readiness.ready").value(true))
            .andReturn();

        mockMvc.perform(put("/api/access/admin/workspace").session(adminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"tenantName":"Phase 2 Updated","description":"Versioned workspace","version":0}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(put("/api/access/admin/workspace").session(adminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"tenantName":"Stale","description":"Must conflict","version":0}
                    """))
            .andExpect(status().isConflict());

        MvcResult created = mockMvc.perform(post("/api/access/admin/workspace/warehouses").session(adminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"code":"wh-south","name":"South","location":"South"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("WH-SOUTH"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.version").value(0))
            .andReturn();
        long warehouseId = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(created.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(put("/api/access/admin/workspace/warehouses/" + warehouseId).session(adminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"name":"South Prime","location":"South Prime","version":0}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(post("/api/access/admin/workspace/warehouses/" + warehouseId + "/retire").session(adminSession)
                .contentType(APPLICATION_JSON).content("{\"version\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/warehouses").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.code == 'WH-SOUTH')]").isEmpty());

        mockMvc.perform(post("/api/access/admin/workspace/warehouses/" + warehouseId + "/reactivate").session(adminSession)
                .contentType(APPLICATION_JSON).content("{\"version\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void duplicateRetirementGuardsLastWarehouseAndScopedOperatorDependencies() throws Exception {
        long coastId = warehouseRepository.findByTenant_CodeIgnoreCaseAndCode(tenant.getCode(), "WH-COAST").orElseThrow().getId();
        long northId = warehouseRepository.findByTenant_CodeIgnoreCaseAndCode(tenant.getCode(), "WH-NORTH").orElseThrow().getId();

        MvcResult operator = mockMvc.perform(post("/api/access/admin/operators").session(adminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"actorName":"Coast Operator","displayName":"Coast Operator","active":true,"roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["WH-COAST"],"tenantWide":false}
                    """))
            .andExpect(status().isOk()).andReturn();
        long operatorId = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(operator.getResponse().getContentAsString()).path("id").asLong();
        // The dependency is intentionally left active: retirement must not detach its scope.
        mockMvc.perform(post("/api/access/admin/workspace/warehouses/" + coastId + "/retire").session(adminSession)
                .contentType(APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isConflict());

        mockMvc.perform(put("/api/access/admin/operators/" + operatorId).session(adminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"actorName":"Coast Operator","displayName":"Coast Operator","active":false,"roles":["INTEGRATION_OPERATOR"],"warehouseScopes":["WH-COAST"],"tenantWide":false,"version":0}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/access/admin/workspace/warehouses/" + coastId + "/retire").session(adminSession)
                .contentType(APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/access/admin/workspace/warehouses/" + northId + "/retire").session(adminSession)
                .contentType(APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isConflict());
    }

    private MockHttpSession signIn() throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"tenantCode":"%s","username":"%s","password":"%s"}
                    """.formatted(tenant.getCode(), ADMIN_USERNAME, ADMIN_PASSWORD)))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
}
