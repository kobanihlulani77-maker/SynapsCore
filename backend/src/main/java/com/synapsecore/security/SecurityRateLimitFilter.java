package com.synapsecore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.audit.RequestTraceContext;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityRateLimitFilter extends OncePerRequestFilter {

    private final SynapseSecurityProperties securityProperties;
    private final SecurityRateLimitService securityRateLimitService;
    private final OperationalMetricsService operationalMetricsService;
    private final RequestTraceContext requestTraceContext;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !securityProperties.getRateLimit().isEnabled() || resolveBucket(request) == null;
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
        boolean allowed = securityRateLimitService.allow(
            bucket.name(),
            principalKey,
            bucket.maxAttempts(),
            bucket.windowSeconds()
        );
        if (allowed) {
            filterChain.doFilter(request, response);
            return;
        }

        operationalMetricsService.recordRateLimitRejection(bucket.name());
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
        return null;
    }

    private String resolvePrincipalKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record BucketDefinition(
        String name,
        int maxAttempts,
        int windowSeconds,
        String rejectionMessage
    ) {
    }
}
