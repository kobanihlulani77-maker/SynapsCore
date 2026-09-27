package com.synapsecore.audit;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.config.SynapseAccessProperties;
import com.synapsecore.observability.OperationalMetricsService;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String ACTOR_MDC_KEY = "actor";
    private static final String TENANT_MDC_KEY = "tenant";
    private static final long SLOW_REQUEST_THRESHOLD_NANOS = 5_000_000_000L;

    private final RequestTraceContext requestTraceContext;
    private final SynapseAccessProperties accessProperties;
    private final AuthSessionService authSessionService;
    private final OperationalMetricsService operationalMetricsService;
    private final DataSource dataSource;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAtNanos = System.nanoTime();
        long identityResolvedAtNanos = -1;
        String incomingRequestId = request.getHeader(RequestTraceContext.REQUEST_ID_HEADER);
        String requestId = incomingRequestId != null && !incomingRequestId.isBlank()
            ? incomingRequestId.trim()
            : UUID.randomUUID().toString();
        String actorName = RequestTraceContext.ANONYMOUS_ACTOR;
        String tenantCode = RequestTraceContext.MISSING_TENANT_CONTEXT;
        int responseStatus = HttpServletResponse.SC_OK;

        requestTraceContext.setCurrentRequestId(requestId);
        requestTraceContext.setCurrentActor(actorName);
        requestTraceContext.setCurrentTenant(tenantCode);
        response.setHeader(RequestTraceContext.REQUEST_ID_HEADER, requestId);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(ACTOR_MDC_KEY, actorName);
        MDC.put(TENANT_MDC_KEY, tenantCode);

        try {
            // Session resolution can acquire a connection; trace and time it too.
            actorName = resolveActorName(request);
            requestTraceContext.setCurrentActor(actorName);
            MDC.put(ACTOR_MDC_KEY, actorName);
            tenantCode = resolveTenantCode(request);
            requestTraceContext.setCurrentTenant(tenantCode);
            MDC.put(TENANT_MDC_KEY, tenantCode);
            identityResolvedAtNanos = System.nanoTime();

            filterChain.doFilter(request, response);
            responseStatus = response.getStatus();
        } catch (IOException | ServletException | RuntimeException exception) {
            if (isAbortLike(exception)) {
                responseStatus = response.isCommitted()
                    ? response.getStatus()
                    : HttpServletResponse.SC_NO_CONTENT;
                if (!response.isCommitted()) {
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
                return;
            }
            responseStatus = response.getStatus() >= 400 ? response.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            throw exception;
        } finally {
            try {
                long elapsedNanos = System.nanoTime() - startedAtNanos;
                operationalMetricsService.recordHttpRequest(
                    tenantCode,
                    request.getMethod(),
                    responseStatus,
                    elapsedNanos
                );
                logSlowRequest(request, responseStatus, elapsedNanos,
                    identityResolvedAtNanos < 0 ? elapsedNanos : identityResolvedAtNanos - startedAtNanos);
            } finally {
                MDC.remove(REQUEST_ID_MDC_KEY);
                MDC.remove(ACTOR_MDC_KEY);
                MDC.remove(TENANT_MDC_KEY);
                requestTraceContext.clear();
            }
        }
    }

    void logSlowRequest(HttpServletRequest request, int status, long elapsedNanos, long identityNanos) {
        if (elapsedNanos < SLOW_REQUEST_THRESHOLD_NANOS || !request.getRequestURI().startsWith("/api/")) {
            return;
        }
        Object matchedRoute = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String route = matchedRoute instanceof String pattern ? pattern : "<unmapped>";
        if ("<unmapped>".equals(route) && isAuthLoginRequest(request)) {
            route = request.getRequestURI();
        }
        int total = -1;
        int active = -1;
        int idle = -1;
        int waiting = -1;
        try {
            if (dataSource instanceof HikariDataSource hikari && hikari.isRunning()) {
                HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
                if (pool != null) {
                    total = pool.getTotalConnections();
                    active = pool.getActiveConnections();
                    idle = pool.getIdleConnections();
                    waiting = pool.getThreadsAwaitingConnection();
                }
            }
        } catch (RuntimeException ignored) {
            // Diagnostics must never change the HTTP outcome during pool shutdown.
        }
        log.warn("Slow HTTP request method={} route={} status={} durationMs={} identityMs={} handlerMs={} hikariTotal={} hikariActive={} hikariIdle={} hikariWaiting={}",
            request.getMethod(), route, status, elapsedNanos / 1_000_000,
            identityNanos / 1_000_000, (elapsedNanos - identityNanos) / 1_000_000,
            total, active, idle, waiting);
    }

    private String resolveActorName(HttpServletRequest request) {
        if (request != null && request.getDispatcherType() == DispatcherType.ERROR) {
            return RequestTraceContext.ANONYMOUS_ACTOR;
        }
        if (isAuthLoginRequest(request)) {
            return RequestTraceContext.ANONYMOUS_ACTOR;
        }
        jakarta.servlet.http.HttpSession session = resolveSessionSafely(request);
        if (session != null && authSessionService.hasSessionIdentity(session)) {
            return authSessionService.resolveAuthenticatedSession(session)
                .map(authenticatedSession -> authenticatedSession.operator().getActorName())
                .orElse(RequestTraceContext.ANONYMOUS_ACTOR);
        }

        if (accessProperties.isAllowHeaderFallback()) {
            String actorHeader = request.getHeader(AccessControlService.ACTOR_HEADER);
            if (actorHeader != null && !actorHeader.isBlank()) {
                return actorHeader.trim();
            }
        }

        return RequestTraceContext.ANONYMOUS_ACTOR;
    }

    private String resolveTenantCode(HttpServletRequest request) {
        if (request != null && request.getDispatcherType() == DispatcherType.ERROR) {
            return RequestTraceContext.MISSING_TENANT_CONTEXT;
        }
        if (isAuthLoginRequest(request)) {
            return RequestTraceContext.MISSING_TENANT_CONTEXT;
        }
        jakarta.servlet.http.HttpSession session = resolveSessionSafely(request);
        if (session != null && authSessionService.hasSessionIdentity(session)) {
            String sessionTenantCode = authSessionService.resolveAuthenticatedSession(session)
                .map(authenticatedSession -> authenticatedSession.tenant().getCode())
                .orElse(null);
            if (sessionTenantCode != null) {
                return sessionTenantCode;
            }
        }

        if (accessProperties.isAllowHeaderFallback()) {
            String tenantHeader = request.getHeader(AccessControlService.TENANT_HEADER);
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                return tenantHeader.trim();
            }
        }

        return RequestTraceContext.MISSING_TENANT_CONTEXT;
    }

    private boolean isAuthLoginRequest(HttpServletRequest request) {
        return request != null
            && "POST".equalsIgnoreCase(request.getMethod())
            && ("/api/auth/session/login".equals(request.getRequestURI())
                || "/api/platform/session/login".equals(request.getRequestURI()));
    }

    private jakarta.servlet.http.HttpSession resolveSessionSafely(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        try {
            return request.getSession(false);
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    private boolean isAbortLike(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("broken pipe")
                    || normalized.contains("connection reset by peer")
                    || normalized.contains("an existing connection was forcibly closed")
                    || normalized.contains("an established connection was aborted")
                    || normalized.contains("session was invalidated")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
