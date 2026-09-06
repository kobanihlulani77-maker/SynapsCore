package com.synapsecore.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.config.SynapseAccessProperties;
import com.synapsecore.config.SynapseRealtimeProperties;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.observability.OperationalMetricsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class RequestTraceFilterTest {

    private final RequestTraceContext trace = new RequestTraceContext();
    private final SynapseAccessProperties access = new SynapseAccessProperties();
    private final StubAuthService auth = new StubAuthService();
    private final RecordingMetrics metrics = new RecordingMetrics();
    private final RequestTraceFilter filter = new RequestTraceFilter(trace, access, auth, metrics);
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboard/snapshot");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearTestContext() {
        trace.clear();
        MDC.clear();
        metrics.registry.close();
    }

    @Test
    void sessionLookupsAlreadyHaveCorrelationAndOnlyResolvedIdentityReachesTheChain() throws Exception {
        request.setSession(new MockHttpSession());
        request.addHeader(RequestTraceContext.REQUEST_ID_HEADER, "  early-lookup  ");
        addSpoofedIdentityHeaders();
        auth.lookup = () -> {
            assertThat(trace.getRequiredRequestId()).isEqualTo("early-lookup");
            assertThat(response.getHeader(RequestTraceContext.REQUEST_ID_HEADER)).isEqualTo("early-lookup");
            assertThat(MDC.get("requestId")).isEqualTo("early-lookup");
            assertThat(trace.getCurrentTenantOrDefault()).isEqualTo(RequestTraceContext.MISSING_TENANT_CONTEXT);
            assertThat(MDC.get("tenant")).isEqualTo(RequestTraceContext.MISSING_TENANT_CONTEXT);
            if (auth.calls == 1) {
                assertThat(trace.getCurrentActorOrAnonymous()).isEqualTo(RequestTraceContext.ANONYMOUS_ACTOR);
            }
            return Optional.of(identity());
        };

        filter.doFilter(request, response, (req, res) -> {
            assertThat(trace.getCurrentActorOrAnonymous()).isEqualTo("Trace Operator");
            assertThat(trace.getCurrentTenantOrDefault()).isEqualTo("TRACE-TENANT");
            assertThat(MDC.get("actor")).isEqualTo("Trace Operator");
            assertThat(MDC.get("tenant")).isEqualTo("TRACE-TENANT");
            response.setStatus(202);
        });

        assertThat(auth.calls).isEqualTo(2);
        assertThat(metrics.tenant).isEqualTo("TRACE-TENANT");
        assertThat(metrics.status).isEqualTo(202);
        assertThat(metrics.method).isEqualTo("GET");
        assertCleared();
    }

    @Test
    void measuredRequestDurationIncludesBothSessionLookups() throws Exception {
        request.setSession(new MockHttpSession());
        long[] lookupWindow = new long[2];
        auth.lookup = () -> {
            if (auth.calls == 1) {
                lookupWindow[0] = System.nanoTime();
            }
            try {
                // Model time spent in session persistence, not a larger production timeout.
                Thread.sleep(50);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            lookupWindow[1] = System.nanoTime();
            return Optional.of(identity());
        };

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(auth.calls).isEqualTo(2);
        assertThat(metrics.durationNanos).isGreaterThanOrEqualTo(lookupWindow[1] - lookupWindow[0]);
        assertCleared();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void resolutionFailureIsCorrelatedMeasuredAndCleanedUp(int failingLookup) {
        request.setSession(new MockHttpSession());
        RuntimeException failure = new IllegalStateException("Connection acquisition failed");
        auth.lookup = () -> {
            if (auth.calls == failingLookup) {
                throw failure;
            }
            return Optional.of(identity());
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("Failed identity resolution must not enter the chain");
        })).isSameAs(failure);

        assertThat(response.getHeader(RequestTraceContext.REQUEST_ID_HEADER)).isNotBlank();
        assertThat(metrics.calls).isEqualTo(1);
        assertThat(metrics.status).isEqualTo(500);
        assertThat(metrics.tenant).isEqualTo(RequestTraceContext.MISSING_TENANT_CONTEXT);
        assertThat(metrics.durationNanos).isPositive();
        assertCleared();
    }

    @Test
    void metricsFailureCannotLeakRequestIdentityOntoTheNextRequest() throws Exception {
        request.addHeader(RequestTraceContext.REQUEST_ID_HEADER, "first-request");
        metrics.failure = new IllegalStateException("Metrics unavailable");

        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> { }))
            .isSameAs(metrics.failure);
        assertCleared();

        metrics.failure = null;
        MockHttpServletResponse nextResponse = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/auth/session"), nextResponse, (req, res) -> {
            assertThat(trace.getRequiredRequestId()).isNotEqualTo("first-request");
            assertThat(trace.getCurrentActorOrAnonymous()).isEqualTo(RequestTraceContext.ANONYMOUS_ACTOR);
        });
        assertThat(UUID.fromString(nextResponse.getHeader(RequestTraceContext.REQUEST_ID_HEADER))).isNotNull();
        assertCleared();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/session/login", "/api/platform/session/login"})
    void loginStillSkipsStaleSessionResolutionAndUntrustedIdentityHeaders(String path) throws Exception {
        request.setMethod("POST");
        request.setRequestURI(path);
        request.setSession(new MockHttpSession());
        access.setAllowHeaderFallback(true);
        addSpoofedIdentityHeaders();
        auth.lookup = () -> { throw new AssertionError("Login must not resolve the old session"); };

        filter.doFilter(request, response, (req, res) -> {
            assertThat(trace.getRequiredRequestId()).isEqualTo(response.getHeader(RequestTraceContext.REQUEST_ID_HEADER));
            assertThat(trace.getCurrentActorOrAnonymous()).isEqualTo(RequestTraceContext.ANONYMOUS_ACTOR);
            assertThat(trace.getCurrentTenantOrDefault()).isEqualTo(RequestTraceContext.MISSING_TENANT_CONTEXT);
        });

        assertThat(auth.calls).isZero();
        assertThat(metrics.status).isEqualTo(200);
        assertCleared();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void headerFallbackRemainsExplicitlyControlled(boolean allowed) throws Exception {
        access.setAllowHeaderFallback(allowed);
        addSpoofedIdentityHeaders();

        filter.doFilter(request, response, (req, res) -> {
            assertThat(trace.getCurrentTenantOrDefault()).isEqualTo(allowed ? "HEADER-TENANT" : RequestTraceContext.MISSING_TENANT_CONTEXT);
            assertThat(trace.getCurrentActorOrAnonymous()).isEqualTo(allowed ? "Header Actor" : RequestTraceContext.ANONYMOUS_ACTOR);
        });

        assertThat(auth.calls).isZero();
        assertCleared();
    }

    @Test
    void downstreamErrorKeepsCorrelationAndFailureMetrics() {
        IOException failure = new IOException("Unrelated downstream failure");
        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            response.setStatus(503);
            throw failure;
        })).isSameAs(failure);

        assertThat(response.getHeader(RequestTraceContext.REQUEST_ID_HEADER)).isNotBlank();
        assertThat(metrics.status).isEqualTo(503);
        assertCleared();
    }

    @Test
    void clientDisconnectRetainsExistingAbortHandlingAndCleanup() throws Exception {
        filter.doFilter(request, response, (req, res) -> { throw new IOException("Broken pipe"); });

        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(metrics.status).isEqualTo(204);
        assertCleared();
    }

    @Test
    void errorDispatchDoesNotAttemptSessionResolution() throws Exception {
        request.setDispatcherType(DispatcherType.ERROR);
        request.setSession(new MockHttpSession());
        auth.lookup = () -> { throw new AssertionError("Error dispatch must not resolve identity again"); };

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(trace.getCurrentTenantOrDefault()).isEqualTo(RequestTraceContext.MISSING_TENANT_CONTEXT);
            assertThat(trace.getCurrentActorOrAnonymous()).isEqualTo(RequestTraceContext.ANONYMOUS_ACTOR);
        });

        assertThat(auth.calls).isZero();
        assertCleared();
    }

    private void addSpoofedIdentityHeaders() {
        request.addHeader(AccessControlService.ACTOR_HEADER, "Header Actor");
        request.addHeader(AccessControlService.TENANT_HEADER, "HEADER-TENANT");
    }

    private void assertCleared() {
        assertThat(trace.getCurrentRequestId()).isEmpty();
        assertThat(trace.getCurrentActor()).isEmpty();
        assertThat(trace.getCurrentTenant()).isEmpty();
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("actor")).isNull();
        assertThat(MDC.get("tenant")).isNull();
    }

    private static AuthSessionService.AuthenticatedSession identity() {
        return new AuthSessionService.AuthenticatedSession(null,
            AccessOperator.builder().actorName("Trace Operator").build(),
            Tenant.builder().code("TRACE-TENANT").build(), null, null, null, false, false);
    }

    private static class StubAuthService extends AuthSessionService {
        private int calls;
        private Supplier<Optional<AuthenticatedSession>> lookup = Optional::empty;

        StubAuthService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public boolean hasSessionIdentity(HttpSession session) {
            return true;
        }

        @Override
        public Optional<AuthenticatedSession> resolveAuthenticatedSession(HttpSession session) {
            calls++;
            return lookup.get();
        }
    }

    private static class RecordingMetrics extends OperationalMetricsService {
        private final SimpleMeterRegistry registry;
        private int calls;
        private String tenant;
        private String method;
        private int status;
        private long durationNanos;
        private RuntimeException failure;

        RecordingMetrics() {
            this(new SimpleMeterRegistry());
        }

        private RecordingMetrics(SimpleMeterRegistry registry) {
            super(registry, new SynapseRealtimeProperties(), null, null, null, null);
            this.registry = registry;
        }

        @Override
        public void recordHttpRequest(String tenantCode, String httpMethod, int statusCode, long duration) {
            calls++;
            tenant = tenantCode;
            method = httpMethod;
            status = statusCode;
            durationNanos = duration;
            if (failure != null) {
                throw failure;
            }
        }
    }
}
