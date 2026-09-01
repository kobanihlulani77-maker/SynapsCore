package com.synapsecore;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;
import com.synapsecore.access.SynapseAccessRole;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.AccessUserRepository;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.IntegrationInboundRecordRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ProductRepository;
import com.synapsecore.domain.repository.RecommendationRepository;
import com.synapsecore.domain.repository.ScenarioRunRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "synapsecore.starter.require-explicit-tenant-provisioning=true",
    "synapsecore.starter.seed-starter-inventory-on-tenant-onboarding=false",
    "synapsecore.starter.seed-starter-connectors-on-tenant-onboarding=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Domain11ControlledProvisioningIntegrationTest {

    private static final String PLATFORM_ADMIN_TOKEN = "test-only-platform-admin-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessOperatorRepository accessOperatorRepository;

    @Autowired
    private AccessUserRepository accessUserRepository;

    @Autowired
    private AlertRepository alertRepository;

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

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Test
    void anonymousProvisioningIsDenied() throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .contentType(APPLICATION_JSON)
                .content(provisioningPayload(uniqueCode(), false)))
            .andExpect(status().isForbidden());
    }

    @Test
    void controlledProvisioningUsesExplicitWarehousesRolesScopesAndOneTimeCredentialHandoff() throws Exception {
        String tenantCode = uniqueCode();

        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(provisioningPayload(tenantCode, false)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value(tenantCode))
            .andExpect(jsonPath("$.readiness").value("READY"))
            .andExpect(jsonPath("$.starterWarehouseCodes").isArray())
            .andExpect(jsonPath("$.starterWarehouseCodes").value(org.hamcrest.Matchers.hasItems("ALPHA-DC", "BETA-DC")))
            .andExpect(jsonPath("$.provisionedUsers[?(@.username == 'domain11.admin')].temporaryCredentialIssued")
                .value(org.hamcrest.Matchers.hasItem(false)))
            .andExpect(jsonPath("$.provisionedUsers[?(@.username == 'domain11.review.alpha')].temporaryCredentialIssued")
                .value(org.hamcrest.Matchers.hasItem(true)))
            .andExpect(jsonPath("$.provisionedUsers[?(@.username == 'domain11.review.alpha')].temporaryCredential")
                .isNotEmpty())
            .andExpect(jsonPath("$.provisionedUsers[?(@.username == 'domain11.review.alpha')].passwordHash").doesNotExist());
    }

    @Test
    void duplicateTenantCodeReturnsConflict() throws Exception {
        String tenantCode = uniqueCode();
        String payload = provisioningPayload(tenantCode, false);

        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isConflict());
    }

    @Test
    void invalidWarehouseScopeAndPlatformOwnerRoleAreRejected() throws Exception {
        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(provisioningPayload(uniqueCode(), true)))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(platformOwnerPayload(uniqueCode())))
            .andExpect(status().isBadRequest());
    }

    @Test
    void oneWarehouseProvisioningCreatesExactlyRequestedConfigurationAndNoOperationalState() throws Exception {
        String tenantCode = uniqueCode();
        String payload = simplePayload(tenantCode,
            "{\"code\":\"ALPHA-DC\",\"name\":\"Alpha Distribution Centre\",\"location\":\"Pretoria\"}",
            minimalUsers(), "");

        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.starterWarehouseCodes").value(org.hamcrest.Matchers.contains("ALPHA-DC")))
            .andExpect(jsonPath("$.provisionedUsers", org.hamcrest.Matchers.hasSize(2)));

        assertExactConfiguration(tenantCode, new String[] {"ALPHA-DC"}, new String[] {"fresh.admin", "fresh.integration"});
        assertNoOperationalState(tenantCode);
    }

    @Test
    void threeWarehouseProvisioningCreatesExactlyRequestedWarehouses() throws Exception {
        String tenantCode = uniqueCode();
        String payload = simplePayload(tenantCode,
            "{\"code\":\"ALPHA-DC\",\"name\":\"Alpha\",\"location\":\"Pretoria\"},"
                + "{\"code\":\"BETA-DC\",\"name\":\"Beta\",\"location\":\"Durban\"},"
                + "{\"code\":\"GAMMA-DC\",\"name\":\"Gamma\",\"location\":\"Gqeberha\"}",
            minimalUsers(), "");

        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.starterWarehouseCodes", org.hamcrest.Matchers.containsInAnyOrder("ALPHA-DC", "BETA-DC", "GAMMA-DC")));

        assertExactConfiguration(tenantCode, new String[] {"ALPHA-DC", "BETA-DC", "GAMMA-DC"},
            new String[] {"fresh.admin", "fresh.integration"});
    }

    @Test
    void explicitReviewOwnerScopeIsStoredOnlyForAssignedWarehouse() throws Exception {
        String tenantCode = uniqueCode();
        String payload = simplePayload(tenantCode,
            "{\"code\":\"ALPHA-DC\",\"name\":\"Alpha\",\"location\":\"Pretoria\"},"
                + "{\"code\":\"BETA-DC\",\"name\":\"Beta\",\"location\":\"Durban\"}",
            """
            {"username":"fresh.admin","fullName":"Fresh Admin","operatorActorName":"Fresh Admin","roles":["TENANT_ADMIN"],"warehouseScopes":[],"initialPassword":"FreshAdmin!123"},
            {"username":"fresh.review","fullName":"Alpha Review Owner","operatorActorName":"Alpha Review Owner","roles":["REVIEW_OWNER"],"warehouseScopes":["ALPHA-DC"]}
            """, "");

        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        AccessOperator reviewOwner = accessOperatorRepository
            .findByTenant_CodeIgnoreCaseAndActorNameIgnoreCaseAndActiveTrue(tenantCode, "Alpha Review Owner")
            .orElseThrow();
        assertThat(reviewOwner.getWarehouseScopes()).containsExactly("ALPHA-DC");
    }

    @Test
    void onlyExplicitTenantAdminAndIntegrationAdminUsersAreCreated() throws Exception {
        String tenantCode = uniqueCode();
        String payload = simplePayload(tenantCode,
            "{\"code\":\"ALPHA-DC\",\"name\":\"Alpha\",\"location\":\"Pretoria\"}",
            minimalUsers(), "");

        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        assertThat(accessUserRepository.findAllByTenant_CodeIgnoreCaseOrderByFullNameAscUsernameAsc(tenantCode))
            .extracting("username")
            .containsExactlyInAnyOrder("fresh.admin", "fresh.integration");
        assertThat(accessOperatorRepository.findAllByTenant_CodeIgnoreCaseAndActiveTrueOrderByDisplayNameAsc(tenantCode))
            .noneMatch(operator -> operator.getRoles().stream().anyMatch(role -> role == SynapseAccessRole.REVIEW_OWNER
                || role == SynapseAccessRole.FINAL_APPROVER || role == SynapseAccessRole.ESCALATION_OWNER));
    }

    @Test
    void missingExplicitlyRequiredRoleFailsReadinessWithoutCreatingSyntheticUser() throws Exception {
        String tenantCode = uniqueCode();
        String payload = simplePayload(tenantCode,
            "{\"code\":\"ALPHA-DC\",\"name\":\"Alpha\",\"location\":\"Pretoria\"}",
            "{\"username\":\"fresh.admin\",\"fullName\":\"Fresh Admin\",\"operatorActorName\":\"Fresh Admin\",\"roles\":[\"TENANT_ADMIN\"],\"warehouseScopes\":[],\"initialPassword\":\"FreshAdmin!123\"}",
            "FINAL_APPROVER");

        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest());

        assertThat(tenantRepository.findByCodeIgnoreCase(tenantCode)).isEmpty();
    }

    private String uniqueCode() {
        return "D11-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String provisioningPayload(String tenantCode, boolean invalidScope) {
        String reviewScope = invalidScope ? "MISSING-DC" : "ALPHA-DC";
        return """
            {
              "tenantCode": "%s",
              "tenantName": "Domain 11 Controlled Company",
              "description": "Synthetic controlled provisioning fixture.",
              "adminFullName": "Domain 11 Tenant Admin",
              "adminUsername": "domain11.admin",
              "adminPassword": "Domain11Admin!123",
              "primaryLocation": "Johannesburg",
              "secondaryLocation": "Cape Town",
              "warehouses": [
                {"code": "ALPHA-DC", "name": "Alpha Distribution Centre", "location": "Johannesburg"},
                {"code": "BETA-DC", "name": "Beta Distribution Centre", "location": "Cape Town"}
              ],
              "users": [
                {
                  "username": "domain11.admin",
                  "fullName": "Domain 11 Tenant Admin",
                  "operatorActorName": "Domain 11 Tenant Admin",
                  "roles": ["TENANT_ADMIN"],
                  "warehouseScopes": [],
                  "initialPassword": "Domain11Admin!123"
                },
                {
                  "username": "domain11.review.alpha",
                  "fullName": "Domain 11 Alpha Review Owner",
                  "operatorActorName": "Domain 11 Alpha Review Owner",
                  "roles": ["REVIEW_OWNER"],
                  "warehouseScopes": ["%s"]
                },
                {
                  "username": "domain11.review.beta",
                  "fullName": "Domain 11 Beta Review Owner",
                  "operatorActorName": "Domain 11 Beta Review Owner",
                  "roles": ["REVIEW_OWNER"],
                  "warehouseScopes": ["BETA-DC"]
                },
                {
                  "username": "domain11.final.alpha",
                  "fullName": "Domain 11 Alpha Final Approver",
                  "operatorActorName": "Domain 11 Alpha Final Approver",
                  "roles": ["FINAL_APPROVER"],
                  "warehouseScopes": ["ALPHA-DC"]
                },
                {
                  "username": "domain11.final.beta",
                  "fullName": "Domain 11 Beta Final Approver",
                  "operatorActorName": "Domain 11 Beta Final Approver",
                  "roles": ["FINAL_APPROVER"],
                  "warehouseScopes": ["BETA-DC"]
                }
              ],
              "requiredRoles": ["REVIEW_OWNER", "FINAL_APPROVER"]
            }
            """.formatted(tenantCode, reviewScope);
    }

    private String minimalUsers() {
        return """
            {"username":"fresh.admin","fullName":"Fresh Admin","operatorActorName":"Fresh Admin","roles":["TENANT_ADMIN"],"warehouseScopes":[],"initialPassword":"FreshAdmin!123"},
            {"username":"fresh.integration","fullName":"Fresh Integration Admin","operatorActorName":"Fresh Integration Admin","roles":["INTEGRATION_ADMIN"],"warehouseScopes":["ALPHA-DC"]}
            """;
    }

    private String simplePayload(String tenantCode, String warehouseJson, String userJson, String requiredRolesJson) {
        return """
            {
              "tenantCode": "%s",
              "tenantName": "Fresh Controlled Company",
              "description": "Synthetic fresh provisioning fixture.",
              "adminFullName": "Fresh Admin",
              "adminUsername": "fresh.admin",
              "adminPassword": "FreshAdmin!123",
              "primaryLocation": "Pretoria",
              "warehouses": [%s],
              "users": [%s],
              "requiredRoles": [%s]
            }
            """.formatted(tenantCode, warehouseJson, userJson, requiredRolesJson);
    }

    private void assertExactConfiguration(String tenantCode, String[] warehouseCodes, String[] usernames) {
        assertThat(warehouseRepository.findAllByTenant_CodeIgnoreCaseOrderByNameAsc(tenantCode))
            .extracting("code")
            .containsExactlyInAnyOrder(warehouseCodes);
        assertThat(accessUserRepository.findAllByTenant_CodeIgnoreCaseOrderByFullNameAscUsernameAsc(tenantCode))
            .extracting("username")
            .containsExactlyInAnyOrder(usernames);
    }

    private void assertNoOperationalState(String tenantCode) {
        assertThat(productRepository.findAllByTenant_CodeIgnoreCaseOrderByNameAsc(tenantCode)).isEmpty();
        assertThat(inventoryRepository.countByTenantCode(tenantCode)).isZero();
        assertThat(integrationConnectorRepository.countByTenant_CodeIgnoreCase(tenantCode)).isZero();
        assertThat(customerOrderRepository.findAll().stream()
            .filter(order -> order.getTenant() != null && tenantCode.equalsIgnoreCase(order.getTenant().getCode())))
            .isEmpty();
        assertThat(fulfillmentTaskRepository.findAll().stream()
            .filter(task -> task.getTenant() != null && tenantCode.equalsIgnoreCase(task.getTenant().getCode())))
            .isEmpty();
        assertThat(alertRepository.findAllByTenant_CodeIgnoreCaseOrderByUpdatedAtDesc(tenantCode)).isEmpty();
        assertThat(recommendationRepository.findAll().stream()
            .filter(recommendation -> recommendation.getTenant() != null && tenantCode.equalsIgnoreCase(recommendation.getTenant().getCode())))
            .isEmpty();
        assertThat(scenarioRunRepository.findTop12ByTenant_CodeIgnoreCaseOrderByCreatedAtDescIdDesc(tenantCode)).isEmpty();
        assertThat(integrationInboundRecordRepository.findAll().stream()
            .filter(record -> tenantCode.equalsIgnoreCase(record.getTenantCode())))
            .isEmpty();
        assertThat(integrationReplayRecordRepository.findAll().stream()
            .filter(record -> tenantCode.equalsIgnoreCase(record.getTenantCode())))
            .isEmpty();
    }

    private String platformOwnerPayload(String tenantCode) {
        return provisioningPayload(tenantCode, false).replace("[\"TENANT_ADMIN\"]", "[\"TENANT_ADMIN\",\"PLATFORM_OWNER\"]");
    }
}
