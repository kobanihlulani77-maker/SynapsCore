package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.auth.StarterAccessUsers;
import com.synapsecore.domain.repository.AccessUserRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.session.store-type=none",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.session.SessionAutoConfiguration",
    "management.health.redis.enabled=false",
    "synapsecore.access.allow-header-fallback=false",
    "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.synapsecore.DashboardSnapshotAuthQueryBoundIntegrationTest$AccessUserSqlInspector"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DashboardSnapshotAuthQueryBoundIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccessUserRepository accessUserRepository;

    public static class AccessUserSqlInspector implements StatementInspector {
        static final AtomicInteger ALL_QUERIES = new AtomicInteger();
        static final AtomicInteger QUERIES = new AtomicInteger();

        @Override
        public String inspect(String sql) {
            ALL_QUERIES.incrementAndGet();
            if (sql.toLowerCase(Locale.ROOT).contains("access_users")) {
                QUERIES.incrementAndGet();
            }
            return sql;
        }
    }

    @Test
    void snapshotReusesValidatedIdentityOnlyWithinOneAuthorizedRead() throws Exception {
        var user = accessUserRepository.findByTenant_CodeIgnoreCaseAndUsernameIgnoreCaseAndActiveTrue(
            StarterAccessUsers.STARTER_TENANT_CODE, "operations.lead").orElseThrow();
        var tenant = user.getTenant();
        var session = new MockHttpSession();
        session.setAttribute(AuthSessionService.SESSION_USERNAME_KEY, user.getUsername());
        session.setAttribute(AuthSessionService.SESSION_TENANT_CODE_KEY, tenant.getCode());
        session.setAttribute(AuthSessionService.SESSION_ACTOR_KEY, user.getOperator().getActorName());
        session.setAttribute(AuthSessionService.SESSION_AUTHENTICATED_AT_KEY, Instant.now().minusSeconds(30).toString());
        session.setAttribute(AuthSessionService.SESSION_USER_SESSION_VERSION_KEY, user.getSessionVersion());
        session.setAttribute(AuthSessionService.SESSION_TENANT_SECURITY_POLICY_VERSION_KEY,
            tenant.getSecurityPolicyVersion());
        AccessUserSqlInspector.QUERIES.set(0);
        AccessUserSqlInspector.ALL_QUERIES.set(0);

        mockMvc.perform(get("/api/dashboard/snapshot").session(session)).andExpect(status().isOk());

        long lookups = AccessUserSqlInspector.QUERIES.get();
        System.out.printf("Dashboard snapshot SQL: total=%d authUser=%d%n",
            AccessUserSqlInspector.ALL_QUERIES.get(), lookups);
        assertThat(lookups).isBetween(1L, 5L);
        assertThat(AccessUserSqlInspector.ALL_QUERIES.get()).isLessThanOrEqualTo(60);

        AccessUserSqlInspector.QUERIES.set(0);
        mockMvc.perform(get("/api/dashboard/snapshot").session(session)).andExpect(status().isOk());
        assertThat(AccessUserSqlInspector.QUERIES.get()).isBetween(1, 5);

        user.setSessionVersion(user.getSessionVersion() + 1);
        accessUserRepository.saveAndFlush(user);
        mockMvc.perform(get("/api/dashboard/snapshot").session(session))
            .andExpect(status().isForbidden());
    }
}
