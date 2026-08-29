package com.synapsecore;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
              ]
            }
            """.formatted(tenantCode, reviewScope);
    }

    private String platformOwnerPayload(String tenantCode) {
        return provisioningPayload(tenantCode, false).replace("[\"TENANT_ADMIN\"]", "[\"TENANT_ADMIN\",\"PLATFORM_OWNER\"]");
    }
}
