package com.synapsecore;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synapsecore.access.PlatformAdministrationAccessService;
import com.synapsecore.access.BootstrapAccessService;
import com.synapsecore.domain.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

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
    "synapsecore.security.rate-limit.enabled=true",
    "synapsecore.security.rate-limit.auth-login.max-attempts=2",
    "synapsecore.security.rate-limit.auth-login.window-seconds=300",
    "synapsecore.security.rate-limit.tenant-onboarding.max-attempts=1",
    "synapsecore.security.rate-limit.tenant-onboarding.window-seconds=300"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SecurityHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void authLoginEndpointRejectsRequestsAfterConfiguredRateLimitWindowIsExceeded() throws Exception {
        long auditCountBefore = auditLogRepository.count();
        String requestBody = """
            {
              "tenantCode": "PILOT-TENANT",
              "username": "admin.pilot",
              "password": "wrong-password"
            }
            """;

        mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/session/login")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value("Authentication rate limit exceeded. Wait before attempting another sign-in."));

        org.assertj.core.api.Assertions.assertThat(auditLogRepository.count())
            .as("Invalid sign-in attempts should not block on audit-log persistence or inflate the audit table.")
            .isEqualTo(auditCountBefore);
    }

    @Test
    void tenantOnboardingEndpointRejectsFurtherRequestsAfterConfiguredRateLimitWindowIsExceeded() throws Exception {
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
                .header(BootstrapAccessService.BOOTSTRAP_TOKEN_HEADER, "bootstrap-secret")
                .header(PlatformAdministrationAccessService.PLATFORM_ADMIN_TOKEN_HEADER, "platform-admin-secret")
                .contentType(APPLICATION_JSON)
                .content(firstTenant))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("RL-ONE"));

        mockMvc.perform(post("/api/access/tenants")
                .header(PlatformAdministrationAccessService.PLATFORM_ADMIN_TOKEN_HEADER, "platform-admin-secret")
                .contentType(APPLICATION_JSON)
                .content(secondTenant))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value("Tenant onboarding rate limit exceeded. Wait before attempting another tenant bootstrap."));
    }
}
