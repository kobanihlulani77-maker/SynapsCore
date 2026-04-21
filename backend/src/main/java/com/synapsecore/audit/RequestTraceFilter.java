package com.synapsecore.audit;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.config.SynapseAccessProperties;
import com.synapsecore.observability.OperationalMetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String ACTOR_MDC_KEY = "actor";
    private static final String TENANT_MDC_KEY = "tenant";

    private final RequestTraceContext requestTraceContext;
    private final SynapseAccessProperties accessProperties;
    private final AuthSessionService authSessionService;
    private final OperationalMetricsService operationalMetricsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String incomingRequestId = request.getHeader(RequestTraceContext.REQUEST_ID_HEADER);
        String requestId = incomingRequestId != null && !incomingRequestId.isBlank()
            ? incomingRequestId.trim()
            : UUID.randomUUID().toString();
        String actorName = resolveActorName(request);
        String tenantCode = resolveTenantCode(request);
        long startedAtNanos = System.nanoTime();
        int responseStatus = HttpServletResponse.SC_OK;

        requestTraceContext.setCurrentRequestId(requestId);
        requestTraceContext.setCurrentActor(actorName);
        requestTraceContext.setCurrentTenant(tenantCode);
        response.setHeader(RequestTraceContext.REQUEST_ID_HEADER, requestId);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(ACTOR_MDC_KEY, actorName);
        MDC.put(TENANT_MDC_KEY, tenantCode);

        try {
            filterChain.doFilter(request, response);
            responseStatus = response.getStatus();
        } catch (IOException | ServletException | RuntimeException exception) {
            responseStatus = response.getStatus() >= 400 ? response.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            throw exception;
        } finally {
            operationalMetricsService.recordHttpRequest(
                tenantCode,
                request.getMethod(),
                responseStatus,
                System.nanoTime() - startedAtNanos
            );
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(ACTOR_MDC_KEY);
            MDC.remove(TENANT_MDC_KEY);
            requestTraceContext.clear();
        }
    }

    private String resolveActorName(HttpServletRequest request) {
        jakarta.servlet.http.HttpSession session = request.getSession(false);
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
        jakarta.servlet.http.HttpSession session = request.getSession(false);
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
}
