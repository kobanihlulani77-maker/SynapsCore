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

import com.synapsecore.auth.StarterAccessUsers;
import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.domain.repository.AccessUserRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.ScenarioRunRepository;
import java.nio.charset.StandardCharsets;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.session.store-type=none",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.session.SessionAutoConfiguration",
    "management.health.redis.enabled=false",
    "synapsecore.access.allow-header-fallback=false",
    "synapsecore.integration.csv-import.max-bytes=1024"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityVerificationIntegrationTest {

    private static final String TEST_PLATFORM_ADMIN_TOKEN = "test-only-platform-admin-token";

    private static final String SECOND_TENANT_CODE = "ACME-SEC";
    private static final String SECOND_TENANT_ADMIN_USERNAME = "acme.sec.admin";
    private static final String SECOND_TENANT_ADMIN_PASSWORD = "Acme-Sec-Admin-2026";
    private static final String SECOND_TENANT_PRODUCT_SKU = "SKU-ACME-SEC-100";
    private static final String SECOND_TENANT_ORDER_ID = "ACME-ORD-SEC-1001";
    private static final String SECOND_TENANT_REPLAY_SOURCE = "acme_secure_csv";
    private static final String SECOND_TENANT_REPLAY_ORDER_ID = "ACME-RPL-SEC-1001";
    private static final String SECOND_TENANT_INTEGRATION_USERNAME = "acme.integration.lead";
    private static final String SECOND_TENANT_INTEGRATION_TEMPORARY_PASSWORD = "Acme-Integration-Temporary-2026";
    private static final String SECOND_TENANT_INTEGRATION_PASSWORD = "Acme-Integration-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessUserRepository accessUserRepository;

    @Autowired
    private IntegrationReplayRecordRepository integrationReplayRecordRepository;

    @Autowired
    private ScenarioRunRepository scenarioRunRepository;

    @Autowired
    private AuthSessionService authSessionService;

    @Test
    void loginAuthenticatesAnonymousSessionAndLogoutClearsOldIdentity() throws Exception {
        MockHttpSession preLoginSession = new MockHttpSession();
        preLoginSession.setAttribute("probe", "pre-auth");

        MockHttpSession signedInSession = signIn(
            StarterAccessUsers.STARTER_TENANT_CODE,
            "operations.lead",
            "lead-2026",
            preLoginSession
        );

        assertThat(signedInSession).isNotNull();
        assertThat(signedInSession.getAttribute("probe")).isEqualTo("pre-auth");

        mockMvc.perform(get("/api/auth/session").session(preLoginSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.username").value("operations.lead"));

        mockMvc.perform(post("/api/auth/session/logout").session(signedInSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(false));

        mockMvc.perform(get("/api/auth/session").session(signedInSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(false));
    }

    @Test
    void malformedSessionTrustAttributesFailClosed() throws Exception {
        MockHttpSession malformedTimestamp = trustedStarterSession();
        malformedTimestamp.setAttribute(
            com.synapsecore.auth.AuthSessionService.SESSION_AUTHENTICATED_AT_KEY,
            "not-an-instant"
        );
        assertThat(authSessionService.resolveAuthenticatedSession(malformedTimestamp)).isEmpty();
        mockMvc.perform(get("/api/dashboard/summary").session(malformedTimestamp))
            .andExpect(status().isForbidden());

        MockHttpSession missingTimestamp = trustedStarterSession();
        missingTimestamp.removeAttribute(com.synapsecore.auth.AuthSessionService.SESSION_AUTHENTICATED_AT_KEY);
        assertThat(authSessionService.resolveAuthenticatedSession(missingTimestamp)).isEmpty();
        mockMvc.perform(get("/api/dashboard/summary").session(missingTimestamp))
            .andExpect(status().isForbidden());

        MockHttpSession futureTimestamp = trustedStarterSession();
        futureTimestamp.setAttribute(
            com.synapsecore.auth.AuthSessionService.SESSION_AUTHENTICATED_AT_KEY,
            java.time.Instant.now().plusSeconds(3600).toString()
        );
        assertThat(authSessionService.resolveAuthenticatedSession(futureTimestamp)).isEmpty();
        mockMvc.perform(get("/api/dashboard/summary").session(futureTimestamp))
            .andExpect(status().isForbidden());

        MockHttpSession malformedVersion = trustedStarterSession();
        malformedVersion.setAttribute(
            com.synapsecore.auth.AuthSessionService.SESSION_USER_SESSION_VERSION_KEY,
            "not-a-number"
        );
        assertThat(authSessionService.resolveAuthenticatedSession(malformedVersion)).isEmpty();
        mockMvc.perform(get("/api/dashboard/summary").session(malformedVersion))
            .andExpect(status().isForbidden());
    }

    @Test
    void requiredPasswordChangeRestrictsWorkspaceUntilPasswordIsRotated() throws Exception {
        var user = accessUserRepository
            .findByTenant_CodeIgnoreCaseAndUsernameIgnoreCaseAndActiveTrue(
                StarterAccessUsers.STARTER_TENANT_CODE,
                "operations.lead"
            )
            .orElseThrow();
        user.setPasswordChangeRequired(true);
        accessUserRepository.saveAndFlush(user);

        MockHttpSession temporarySession = signIn(
            StarterAccessUsers.STARTER_TENANT_CODE,
            "operations.lead",
            "lead-2026"
        );
        mockMvc.perform(get("/api/auth/session").session(temporarySession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andExpect(jsonPath("$.passwordChangeRequired").value(true));

        for (String endpoint : new String[] {
            "/api/dashboard/summary",
            "/api/inventory",
            "/api/orders/recent",
            "/api/alerts",
            "/api/recommendations"
        }) {
            mockMvc.perform(get(endpoint).session(temporarySession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("Password change is required")));
        }

        mockMvc.perform(post("/api/auth/session/password")
                .session(temporarySession)
                .contentType(APPLICATION_JSON)
                .content("{\"currentPassword\":\"lead-2026\",\"newPassword\":\"lead-rotated-2026\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passwordChangeRequired").value(false));

        mockMvc.perform(get("/api/dashboard/summary").session(temporarySession))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("{\"tenantCode\":\"STARTER-OPS\",\"username\":\"operations.lead\",\"password\":\"lead-2026\"}"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("{\"tenantCode\":\"STARTER-OPS\",\"username\":\"operations.lead\",\"password\":\"lead-rotated-2026\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void signedInNonAdminRolesCannotPerformTenantAdminMutations() throws Exception {
        MockHttpSession plannerSession = signIn(
            StarterAccessUsers.STARTER_TENANT_CODE,
            "operations.planner",
            "planner-2026"
        );
        MockHttpSession integrationAdminSession = signIn(
            StarterAccessUsers.STARTER_TENANT_CODE,
            "integration.lead",
            "integration-admin-2026"
        );
        MockHttpSession operatorSession = signIn(
            StarterAccessUsers.STARTER_TENANT_CODE,
            "operations.operator",
            "operations-operator-2026"
        );

        Long operationsLeadUserId = accessUserRepository
            .findByTenant_CodeIgnoreCaseAndUsernameIgnoreCaseAndActiveTrue(
                StarterAccessUsers.STARTER_TENANT_CODE,
                "operations.lead"
            )
            .orElseThrow()
            .getId();

        mockMvc.perform(post("/api/access/admin/users")
                .session(plannerSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "username": "forbidden.user",
                      "fullName": "Forbidden User",
                      "password": "Forbidden-User-2026",
                      "operatorActorName": "Operations Operator"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(Matchers.containsString("required roles")))
            .andExpect(jsonPath("$.trace").doesNotExist());

        mockMvc.perform(put("/api/access/admin/workspace")
                .session(integrationAdminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantName": "Starter Operations Workspace",
                      "description": "Forbidden integration-admin tenant update."
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(Matchers.containsString("required roles")))
            .andExpect(jsonPath("$.trace").doesNotExist());

        mockMvc.perform(post("/api/access/admin/users/" + operationsLeadUserId + "/reset-password")
                .session(operatorSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "password": "Reset-Blocked-2026"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(Matchers.containsString("required roles")))
            .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void tenantScopedApisAndReplayEndpointsDoNotLeakAcrossTenants() throws Exception {
        TenantIsolationFixture fixture = onboardSecondTenantWithOperationalData();

        mockMvc.perform(get("/api/products")
                .session(fixture.starterAdminSession())
                .header("X-Synapse-Tenant", SECOND_TENANT_CODE))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_PRODUCT_SKU))));

        mockMvc.perform(get("/api/inventory")
                .session(fixture.starterAdminSession())
                .header("X-Synapse-Tenant", SECOND_TENANT_CODE))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_PRODUCT_SKU))));

        mockMvc.perform(get("/api/orders/recent")
                .session(fixture.starterAdminSession())
                .header("X-Synapse-Tenant", SECOND_TENANT_CODE))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_ORDER_ID))));

        mockMvc.perform(get("/api/alerts")
                .session(fixture.starterAdminSession())
                .header("X-Synapse-Tenant", SECOND_TENANT_CODE))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_PRODUCT_SKU))));

        mockMvc.perform(get("/api/recommendations")
                .session(fixture.starterAdminSession())
                .header("X-Synapse-Tenant", SECOND_TENANT_CODE))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_PRODUCT_SKU))));

        mockMvc.perform(get("/api/integrations/orders/replay-queue")
                .session(fixture.starterIntegrationOperatorSession())
                .header("X-Synapse-Tenant", SECOND_TENANT_CODE))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_REPLAY_ORDER_ID))));

        mockMvc.perform(get("/api/integrations/orders/connectors")
                .session(fixture.starterIntegrationOperatorSession())
                .header("X-Synapse-Tenant", SECOND_TENANT_CODE))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_REPLAY_SOURCE))));

        mockMvc.perform(get("/api/dashboard/snapshot")
                .session(fixture.starterAdminSession())
                .header("X-Synapse-Tenant", SECOND_TENANT_CODE))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_REPLAY_ORDER_ID))));

        mockMvc.perform(get("/api/system/runtime")
                .session(fixture.starterAdminSession())
                .header("X-Synapse-Tenant", SECOND_TENANT_CODE))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_REPLAY_SOURCE))));

        mockMvc.perform(get("/api/access/admin/users")
                .session(fixture.starterAdminSession())
                .header("X-Synapse-Tenant", SECOND_TENANT_CODE))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_ADMIN_USERNAME))));

        mockMvc.perform(get("/api/access/admin/workspace")
                .session(fixture.starterAdminSession())
                .header("X-Synapse-Tenant", SECOND_TENANT_CODE))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_CODE))))
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_REPLAY_SOURCE))));

        mockMvc.perform(get("/api/events/recent")
                .session(fixture.starterAdminSession())
                .header("X-Synapse-Tenant", SECOND_TENANT_CODE))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_ORDER_ID))));

        mockMvc.perform(get("/api/audit/recent")
                .session(fixture.starterAdminSession())
                .header("X-Synapse-Tenant", SECOND_TENANT_CODE))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_ADMIN_USERNAME))))
            .andExpect(content().string(Matchers.not(Matchers.containsString(SECOND_TENANT_REPLAY_SOURCE))));

        mockMvc.perform(get("/api/scenarios/history")
                .session(fixture.starterAdminSession())
                .header("X-Synapse-Tenant", SECOND_TENANT_CODE))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.not(
                Matchers.containsString("\"id\":" + fixture.scenarioRunId()))));

        mockMvc.perform(get("/api/scenarios/" + fixture.scenarioRunId() + "/request")
                .session(fixture.starterAdminSession()))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/access/operators")
                .session(fixture.starterAdminSession())
                .param("tenantCode", SECOND_TENANT_CODE))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(
                "Current tenant STARTER-OPS cannot view workspace operators for tenant " + SECOND_TENANT_CODE + "."
            ));

        mockMvc.perform(post("/api/integrations/orders/replay/" + fixture.replayRecordId())
                .session(fixture.starterIntegrationOperatorSession()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(
                "Integration replay record not found: " + fixture.replayRecordId()
            ));

        mockMvc.perform(get("/api/integrations/orders/replay-queue")
                .session(fixture.secondTenantAdminSession()))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString(SECOND_TENANT_REPLAY_ORDER_ID)));
    }

    @Test
    void malformedBodiesInvalidEnumsAndWrongTenantSignInStaySafe() throws Exception {
        MockHttpSession integrationAdminSession = signIn(
            StarterAccessUsers.STARTER_TENANT_CODE,
            "integration.lead",
            "integration-admin-2026"
        );

        mockMvc.perform(post("/api/integrations/orders/connectors")
                .session(integrationAdminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "sourceSystem": "bad_enum_source",
                      "type": "NOT_A_CONNECTOR",
                      "displayName": "Bad Enum Connector",
                      "enabled": true
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Request body is malformed or contains invalid values."))
            .andExpect(jsonPath("$.requestId").isNotEmpty())
            .andExpect(jsonPath("$.trace").doesNotExist());

        mockMvc.perform(post("/api/orders")
                .session(signIn(
                    StarterAccessUsers.STARTER_TENANT_CODE,
                    "operations.lead",
                    "lead-2026"
                ))
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "warehouseCode": "WH-NORTH",
                      "items": [
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Request body is malformed or contains invalid values."))
            .andExpect(jsonPath("$.requestId").isNotEmpty())
            .andExpect(jsonPath("$.trace").doesNotExist());

        mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "UNKNOWN-OPS",
                      "username": "operations.lead",
                      "password": "lead-2026"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid operator credentials."))
            .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void oversizedCsvUploadsAreRejectedSafely() throws Exception {
        MockHttpSession integrationAdminSession = signIn(
            StarterAccessUsers.STARTER_TENANT_CODE,
            "integration.lead",
            "integration-admin-2026"
        );

        MockMultipartFile oversizedCsv = new MockMultipartFile(
            "file",
            "oversized-orders.csv",
            "text/csv",
            buildOversizedCsv(1025).getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/integrations/orders/csv-import")
                .file(oversizedCsv)
                .session(integrationAdminSession)
                .param("sourceSystem", "oversized_feed"))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.message").value("Uploaded file exceeds the configured SynapseCore CSV import size limit."))
            .andExpect(jsonPath("$.requestId").isNotEmpty())
            .andExpect(jsonPath("$.trace").doesNotExist());
    }

    private TenantIsolationFixture onboardSecondTenantWithOperationalData() throws Exception {
        MockHttpSession starterAdminSession = signIn(
            StarterAccessUsers.STARTER_TENANT_CODE,
            "operations.lead",
            "lead-2026"
        );

        mockMvc.perform(post("/api/access/tenants")
                .header("X-Synapse-Platform-Admin-Token", TEST_PLATFORM_ADMIN_TOKEN)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "%s",
                      "tenantName": "Acme Security Tenant",
                      "description": "Tenant isolation verification workspace.",
                      "adminFullName": "Acme Security Admin",
                      "adminUsername": "%s",
                      "adminPassword": "%s",
                      "primaryLocation": "Johannesburg",
                      "secondaryLocation": "Cape Town"
                    }
                    """.formatted(
                    SECOND_TENANT_CODE,
                    SECOND_TENANT_ADMIN_USERNAME,
                    SECOND_TENANT_ADMIN_PASSWORD
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value(SECOND_TENANT_CODE));

        MockHttpSession secondTenantAdminSession = signIn(
            SECOND_TENANT_CODE,
            SECOND_TENANT_ADMIN_USERNAME,
            SECOND_TENANT_ADMIN_PASSWORD
        );

        mockMvc.perform(post("/api/products")
                .session(secondTenantAdminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "sku": "%s",
                      "name": "Acme Security Sensor",
                      "category": "Isolation"
                    }
                    """.formatted(SECOND_TENANT_PRODUCT_SKU)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value(SECOND_TENANT_PRODUCT_SKU));

        mockMvc.perform(post("/api/inventory/update")
                .session(secondTenantAdminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "productSku": "%s",
                      "warehouseCode": "WH-NORTH",
                      "quantityAvailable": 2,
                      "reorderThreshold": 5
                    }
                    """.formatted(SECOND_TENANT_PRODUCT_SKU)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lowStock").value(true));

        mockMvc.perform(post("/api/orders")
                .session(secondTenantAdminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "externalOrderId": "%s",
                      "warehouseCode": "WH-NORTH",
                      "items": [
                        {
                          "productSku": "%s",
                          "quantity": 1,
                          "unitPrice": 199.00
                        }
                      ]
                    }
                    """.formatted(
                    SECOND_TENANT_ORDER_ID,
                    SECOND_TENANT_PRODUCT_SKU
                )))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.externalOrderId").value(SECOND_TENANT_ORDER_ID));

        mockMvc.perform(post("/api/scenarios/order-impact")
                .session(secondTenantAdminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "externalOrderId": null,
                      "warehouseCode": "WH-NORTH",
                      "items": [
                        {
                          "productSku": "%s",
                          "quantity": 1,
                          "unitPrice": 199.00
                        }
                      ]
                    }
                    """.formatted(SECOND_TENANT_PRODUCT_SKU)))
            .andExpect(status().isOk());
        Long secondTenantScenarioRunId = scenarioRunRepository
            .findTop12ByTenant_CodeIgnoreCaseOrderByCreatedAtDescIdDesc(SECOND_TENANT_CODE)
            .stream()
            .findFirst()
            .orElseThrow()
            .getId();

        mockMvc.perform(post("/api/access/admin/operators")
                .session(secondTenantAdminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "actorName": "Acme Integration Lead",
                      "displayName": "Acme Integration Lead",
                      "description": "Second tenant integration admin for isolation verification.",
                      "active": true,
                      "roles": ["INTEGRATION_ADMIN"],
                      "warehouseScopes": ["WH-NORTH"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actorName").value("Acme Integration Lead"));

        mockMvc.perform(post("/api/access/admin/users")
                .session(secondTenantAdminSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "fullName": "Acme Integration Lead",
                      "password": "%s",
                      "operatorActorName": "Acme Integration Lead"
                    }
                    """.formatted(
                    SECOND_TENANT_INTEGRATION_USERNAME,
                    SECOND_TENANT_INTEGRATION_TEMPORARY_PASSWORD
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value(SECOND_TENANT_INTEGRATION_USERNAME));

        MockHttpSession temporaryIntegrationSession = signIn(
            SECOND_TENANT_CODE,
            SECOND_TENANT_INTEGRATION_USERNAME,
            SECOND_TENANT_INTEGRATION_TEMPORARY_PASSWORD
        );
        mockMvc.perform(post("/api/auth/session/password")
                .session(temporaryIntegrationSession)
                .contentType(APPLICATION_JSON)
                .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}"
                    .formatted(
                        SECOND_TENANT_INTEGRATION_TEMPORARY_PASSWORD,
                        SECOND_TENANT_INTEGRATION_PASSWORD
                    )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passwordChangeRequired").value(false));

        MockHttpSession secondTenantIntegrationSession = signIn(
            SECOND_TENANT_CODE,
            SECOND_TENANT_INTEGRATION_USERNAME,
            SECOND_TENANT_INTEGRATION_PASSWORD
        );

        mockMvc.perform(post("/api/integrations/orders/connectors")
                .session(secondTenantIntegrationSession)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "sourceSystem": "%s",
                      "type": "CSV_ORDER_IMPORT",
                      "displayName": "Acme Secure CSV Feed",
                      "enabled": false,
                      "syncMode": "BATCH_FILE_DROP",
                      "validationPolicy": "RELAXED",
                      "transformationPolicy": "NORMALIZE_CODES",
                      "allowDefaultWarehouseFallback": false,
                      "notes": "Disabled for tenant isolation replay verification."
                    }
                    """.formatted(SECOND_TENANT_REPLAY_SOURCE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false));

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "acme-isolation-orders.csv",
            "text/csv",
            """
                sourceSystem,externalOrderId,warehouseCode,productSku,quantity,unitPrice
                %s,%s,WH-NORTH,%s,2,88.00
                """.formatted(
                SECOND_TENANT_REPLAY_SOURCE,
                SECOND_TENANT_REPLAY_ORDER_ID,
                SECOND_TENANT_PRODUCT_SKU
            ).getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/integrations/orders/csv-import")
                .file(file)
                .session(secondTenantIntegrationSession)
                .param("sourceSystem", SECOND_TENANT_REPLAY_SOURCE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ordersImported").value(0))
            .andExpect(jsonPath("$.ordersFailed").value(1))
            .andExpect(jsonPath("$.failedOrders[0].failureCode").value("CONNECTOR_DISABLED"))
            .andExpect(jsonPath("$.failedOrders[0].externalOrderId").value(SECOND_TENANT_REPLAY_ORDER_ID));

        mockMvc.perform(get("/api/alerts")
                .session(secondTenantAdminSession))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString(SECOND_TENANT_PRODUCT_SKU)));

        mockMvc.perform(get("/api/recommendations")
                .session(secondTenantAdminSession))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString(SECOND_TENANT_PRODUCT_SKU)));

        mockMvc.perform(get("/api/dashboard/snapshot")
                .session(secondTenantIntegrationSession))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString(SECOND_TENANT_REPLAY_ORDER_ID)));

        mockMvc.perform(get("/api/system/runtime")
                .session(secondTenantAdminSession))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString(SECOND_TENANT_REPLAY_SOURCE)));

        Long replayRecordId = integrationReplayRecordRepository.findAll().stream()
            .filter(record -> SECOND_TENANT_REPLAY_ORDER_ID.equals(record.getExternalOrderId()))
            .findFirst()
            .orElseThrow()
            .getId();

        MockHttpSession starterIntegrationOperatorSession = signIn(
            StarterAccessUsers.STARTER_TENANT_CODE,
            "integration.operator",
            "integration-ops-2026"
        );

        return new TenantIsolationFixture(
            starterAdminSession,
            starterIntegrationOperatorSession,
            secondTenantAdminSession,
            replayRecordId,
            secondTenantScenarioRunId
        );
    }

    private MockHttpSession signIn(String tenantCode, String username, String password) throws Exception {
        return signIn(tenantCode, username, password, null);
    }

    private MockHttpSession trustedStarterSession() {
        var user = accessUserRepository
            .findByTenant_CodeIgnoreCaseAndUsernameIgnoreCaseAndActiveTrue(
                StarterAccessUsers.STARTER_TENANT_CODE,
                "operations.lead"
            )
            .orElseThrow();
        var tenant = user.getTenant();
        var session = new MockHttpSession();
        session.setAttribute(com.synapsecore.auth.AuthSessionService.SESSION_USERNAME_KEY, user.getUsername());
        session.setAttribute(com.synapsecore.auth.AuthSessionService.SESSION_TENANT_CODE_KEY, tenant.getCode());
        session.setAttribute(com.synapsecore.auth.AuthSessionService.SESSION_ACTOR_KEY, user.getOperator().getActorName());
        session.setAttribute(
            com.synapsecore.auth.AuthSessionService.SESSION_AUTHENTICATED_AT_KEY,
            java.time.Instant.now().minusSeconds(30).toString()
        );
        session.setAttribute(
            com.synapsecore.auth.AuthSessionService.SESSION_USER_SESSION_VERSION_KEY,
            user.getSessionVersion()
        );
        session.setAttribute(
            com.synapsecore.auth.AuthSessionService.SESSION_TENANT_SECURITY_POLICY_VERSION_KEY,
            tenant.getSecurityPolicyVersion()
        );
        return session;
    }

    private MockHttpSession signIn(String tenantCode,
                                   String username,
                                   String password,
                                   MockHttpSession existingSession) throws Exception {
        var requestBuilder = post("/api/auth/session/login")
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(tenantCode, username, password));
        if (existingSession != null) {
            requestBuilder.session(existingSession);
        }
        return (MockHttpSession) mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signedIn").value(true))
            .andReturn()
            .getRequest()
            .getSession(false);
    }

    private String buildOversizedCsv(int minimumBytes) {
        StringBuilder builder = new StringBuilder("""
            sourceSystem,externalOrderId,warehouseCode,productSku,quantity,unitPrice
            oversized_feed,OS-1,WH-NORTH,SKU-PILOT-TENANT-PROOF,1,10.00
            """);
        while (builder.toString().getBytes(StandardCharsets.UTF_8).length <= minimumBytes) {
            builder.append("oversized_feed,OS-")
                .append(builder.length())
                .append(",WH-NORTH,SKU-PILOT-TENANT-PROOF,1,10.00\n");
        }
        return builder.toString();
    }

    private record TenantIsolationFixture(
        MockHttpSession starterAdminSession,
        MockHttpSession starterIntegrationOperatorSession,
        MockHttpSession secondTenantAdminSession,
        Long replayRecordId,
        Long scenarioRunId
    ) {
    }
}
