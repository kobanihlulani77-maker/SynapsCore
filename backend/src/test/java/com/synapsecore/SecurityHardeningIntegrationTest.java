package com.synapsecore;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synapsecore.access.PlatformAdministrationAccessService;
import com.synapsecore.access.BootstrapAccessService;
import com.synapsecore.domain.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.profiles.active=prod",
    "spring.datasource.url=jdbc:h2:mem:securityhardening;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.data.redis.url=redis://localhost:6379",
    "management.health.redis.enabled=false",
    "synapsecore.realtime.broker-mode=SIMPLE_IN_MEMORY",
    "synapsecore.bootstrap.initial-token=bootstrap-secret",
    "synapsecore.bootstrap.platform-admin-token=platform-admin-secret",
    "synapsecore.cors.allowed-origins=https://synapscore-frontend-3.onrender.com",
    "synapsecore.security.rate-limit.enabled=true",
    "synapsecore.security.rate-limit.auth-login.max-attempts=2",
    "synapsecore.security.rate-limit.auth-login.window-seconds=300",
    "synapsecore.security.rate-limit.tenant-onboarding.max-attempts=1",
    "synapsecore.security.rate-limit.tenant-onboarding.window-seconds=300",
    "synapsecore.security.rate-limit.access-admin-mutation.max-attempts=1",
    "synapsecore.security.rate-limit.access-admin-mutation.window-seconds=300",
    "synapsecore.security.rate-limit.integration-mutation.max-attempts=1",
    "synapsecore.security.rate-limit.integration-mutation.window-seconds=300"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SecurityHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void resetAuditStateExpectation() {
        org.assertj.core.api.Assertions.assertThat(auditLogRepository).isNotNull();
    }

    @Test
    void authLoginEndpointRejectsRequestsQuicklyWithoutCreatingSessionsOrAuditWrites() throws Exception {
        long auditCountBefore = auditLogRepository.count();
        String forwardedFor = "203.0.113.10";
        String origin = "https://synapscore-frontend-3.onrender.com";
        String requestBody = """
            {
              "tenantCode": "PILOT-TENANT",
              "username": "admin.pilot",
              "password": "wrong-password"
            }
            """;

        MvcResult firstAttempt = mockMvc.perform(post("/api/auth/session/login")
                .header("X-Forwarded-For", forwardedFor)
                .header("Origin", origin)
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid operator credentials."))
            .andReturn();

        org.assertj.core.api.Assertions.assertThat(firstAttempt.getRequest().getSession(false))
            .as("Failed login should not create a server session before credentials are verified.")
            .isNull();
        org.assertj.core.api.Assertions.assertThat(firstAttempt.getResponse().getHeader("Set-Cookie"))
            .as("Failed login should not emit a session cookie.")
            .isNull();

        mockMvc.perform(post("/api/auth/session/login")
                .header("X-Forwarded-For", forwardedFor)
                .header("Origin", origin)
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/session/login")
                .header("X-Forwarded-For", forwardedFor)
                .header("Origin", origin)
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isTooManyRequests())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", origin))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Access-Control-Allow-Credentials", "true"))
            .andExpect(jsonPath("$.message").value("Authentication rate limit exceeded. Wait before attempting another sign-in."));

        org.assertj.core.api.Assertions.assertThat(auditLogRepository.count())
            .as("Invalid sign-in attempts should not block on audit-log persistence or inflate the audit table.")
            .isEqualTo(auditCountBefore);
    }

    @Test
    void tenantOnboardingEndpointRejectsFurtherRequestsAfterConfiguredRateLimitWindowIsExceeded() throws Exception {
        String forwardedFor = "203.0.113.20";
        String firstTenant = """
            {
              "tenantCode": "RL-ONE",
              "tenantName": "Rate Limit One",
              "description": "Security hardening proof.",
              "adminFullName": "Rate Limit Admin",
              "adminUsername": "rate.limit.one",
              "adminPassword": "Admin@123",
              "primaryLocation": "Johannesburg",
              "secondaryLocation": "Cape Town"
            }
            """;
        String secondTenant = firstTenant.replace("RL-ONE", "RL-TWO").replace("Rate Limit One", "Rate Limit Two").replace("rate.limit.one", "rate.limit.two");

        mockMvc.perform(post("/api/access/tenants")
                .header("X-Forwarded-For", forwardedFor)
                .header(BootstrapAccessService.BOOTSTRAP_TOKEN_HEADER, "bootstrap-secret")
                .header(PlatformAdministrationAccessService.PLATFORM_ADMIN_TOKEN_HEADER, "platform-admin-secret")
                .contentType(APPLICATION_JSON)
                .content(firstTenant))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("RL-ONE"));

        mockMvc.perform(post("/api/access/tenants")
                .header("X-Forwarded-For", forwardedFor)
                .header(PlatformAdministrationAccessService.PLATFORM_ADMIN_TOKEN_HEADER, "platform-admin-secret")
                .contentType(APPLICATION_JSON)
                .content(secondTenant))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value("Tenant onboarding rate limit exceeded. Wait before attempting another tenant bootstrap."));
    }

    @Test
    void accessAdminMutationsAreRateLimitedConsistently() throws Exception {
        String forwardedFor = "203.0.113.30";

        mockMvc.perform(put("/api/access/admin/workspace")
                .header("X-Forwarded-For", forwardedFor)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantName": "Pilot Tenant",
                      "description": "Security hardening update."
                    }
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/access/admin/workspace")
                .header("X-Forwarded-For", forwardedFor)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "tenantName": "Pilot Tenant",
                      "description": "Security hardening update."
                    }
                    """))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value("Workspace administration rate limit exceeded. Wait before attempting another tenant administration change."));
    }

    @Test
    void operationalPolicyMutationsShareTheTenantAdminRateLimitBucket() throws Exception {
        String forwardedFor = "203.0.113.31";

        mockMvc.perform(put("/api/system/policy")
                .header("X-Forwarded-For", forwardedFor)
                .contentType(APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/system/policy")
                .header("X-Forwarded-For", forwardedFor)
                .contentType(APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value("Workspace administration rate limit exceeded. Wait before attempting another tenant administration change."));
    }

    @Test
    void integrationMutationEndpointsAreRateLimitedBeforeControllerLogicRuns() throws Exception {
        String forwardedFor = "203.0.113.40";

        mockMvc.perform(post("/api/integrations/orders/webhook")
                .header("X-Forwarded-For", forwardedFor)
                .contentType(APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/integrations/orders/webhook")
                .header("X-Forwarded-For", forwardedFor)
                .contentType(APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value("Integration mutation rate limit exceeded. Wait before attempting another connector, webhook, import, or replay change."));
    }
}
