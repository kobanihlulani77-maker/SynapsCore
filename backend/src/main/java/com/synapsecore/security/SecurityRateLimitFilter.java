package com.synapsecore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.config.SynapseCorsProperties;
import com.synapsecore.config.SynapseSecurityProperties;
import com.synapsecore.observability.OperationalMetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityRateLimitFilter extends OncePerRequestFilter {

    private static final java.util.Set<String> MUTATING_METHODS = java.util.Set.of("POST", "PUT", "PATCH", "DELETE");

    private final SynapseSecurityProperties securityProperties;
    private final SynapseCorsProperties corsProperties;
    private final SecurityRateLimitService securityRateLimitService;
    private final OperationalMetricsService operationalMetricsService;
    private final RequestTraceContext requestTraceContext;
    private final ObjectMapper objectMapper;
    private final AuthSessionService authSessionService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !securityProperties.getRateLimit().isEnabled() || resolveBucket(request) == null;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        BucketDefinition bucket = resolveBucket(request);
        if (bucket == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String principalKey = resolvePrincipalKey(request);
        SecurityRateLimitService.RateLimitDecision decision = securityRateLimitService.evaluate(
            bucket.name(),
            principalKey,
            bucket.maxAttempts(),
            bucket.windowSeconds()
        );
        applyRateLimitHeaders(response, bucket, decision);
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        operationalMetricsService.recordRateLimitRejection(bucket.name());
        applyCorsHeaders(request, response);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
            "timestamp", Instant.now().toString(),
            "status", 429,
            "error", "Too Many Requests",
            "message", bucket.rejectionMessage(),
            "requestId", requestTraceContext.getRequiredRequestId()
        ));
    }

    private void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isBlank()) {
            return;
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
        configuration.setAllowCredentials(true);
        String allowedOrigin = configuration.checkOrigin(origin);
        if (allowedOrigin == null) {
            return;
        }

        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
    }

    private void applyRateLimitHeaders(HttpServletResponse response,
                                       BucketDefinition bucket,
                                       SecurityRateLimitService.RateLimitDecision decision) {
        response.setHeader("X-Synapse-RateLimit-Bucket", bucket.name());
        response.setHeader("X-Synapse-RateLimit-Limit", String.valueOf(decision.maxAttempts()));
        response.setHeader("X-Synapse-RateLimit-Remaining", String.valueOf(decision.remainingAttempts()));
        response.setHeader("X-Synapse-RateLimit-Reset-After-Seconds", String.valueOf(decision.retryAfterSeconds()));
    }

    private BucketDefinition resolveBucket(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        SynapseSecurityProperties.RateLimitProperties properties = securityProperties.getRateLimit();
        if ("POST".equalsIgnoreCase(method) && "/api/auth/session/login".equals(path)) {
            return new BucketDefinition(
                "AUTH_LOGIN",
                properties.getAuthLogin().getMaxAttempts(),
                properties.getAuthLogin().getWindowSeconds(),
                "Authentication rate limit exceeded. Wait before attempting another sign-in."
            );
        }
        if ("POST".equalsIgnoreCase(method) && "/api/auth/session/password".equals(path)) {
            return new BucketDefinition(
                "AUTH_PASSWORD_CHANGE",
                properties.getAuthPassword().getMaxAttempts(),
                properties.getAuthPassword().getWindowSeconds(),
                "Password change rate limit exceeded. Wait before attempting another password update."
            );
        }
        if ("POST".equalsIgnoreCase(method) && "/api/access/tenants".equals(path)) {
            return new BucketDefinition(
                "TENANT_ONBOARDING",
                properties.getTenantOnboarding().getMaxAttempts(),
                properties.getTenantOnboarding().getWindowSeconds(),
                "Tenant onboarding rate limit exceeded. Wait before attempting another tenant bootstrap."
            );
        }
        if (isMutatingAccessAdminRequest(method, path) || isMutatingSystemAdminRequest(method, path)) {
            return new BucketDefinition(
                "ACCESS_ADMIN_MUTATION",
                properties.getAccessAdminMutation().getMaxAttempts(),
                properties.getAccessAdminMutation().getWindowSeconds(),
                "Workspace administration rate limit exceeded. Wait before attempting another tenant administration change."
            );
        }
        if (isMutatingIntegrationRequest(method, path)) {
            return new BucketDefinition(
                "INTEGRATION_MUTATION",
                properties.getIntegrationMutation().getMaxAttempts(),
                properties.getIntegrationMutation().getWindowSeconds(),
                "Integration mutation rate limit exceeded. Wait before attempting another connector, webhook, import, or replay change."
            );
        }
        return null;
    }

    private String resolvePrincipalKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String remoteAddress = request.getRemoteAddr();
        String networkKey = remoteAddress;
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            networkKey = forwardedFor.split(",")[0].trim();
        }
        try {
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            if (session != null) {
                var authenticatedSession = authSessionService.resolveAuthenticatedSession(session).orElse(null);
                if (authenticatedSession != null) {
                    return ("session:" + authenticatedSession.tenant().getCode()
                        + ":" + authenticatedSession.user().getUsername()
                        + ":" + networkKey).toUpperCase(java.util.Locale.ROOT);
                }
            }
        } catch (IllegalStateException ignored) {
            return networkKey;
        }
        return networkKey;
    }

    private boolean isMutatingAccessAdminRequest(String method, String path) {
        return path != null
            && path.startsWith("/api/access/admin/")
            && MUTATING_METHODS.contains(method.toUpperCase(java.util.Locale.ROOT));
    }

    private boolean isMutatingIntegrationRequest(String method, String path) {
        return path != null
            && path.startsWith("/api/integrations/orders/")
            && MUTATING_METHODS.contains(method.toUpperCase(java.util.Locale.ROOT));
    }

    private boolean isMutatingSystemAdminRequest(String method, String path) {
        return path != null
            && path.startsWith("/api/system/policy")
            && MUTATING_METHODS.contains(method.toUpperCase(java.util.Locale.ROOT));
    }

    private record BucketDefinition(
        String name,
        int maxAttempts,
        int windowSeconds,
        String rejectionMessage
    ) {
    }
}
