package com.synapsecore.security;

import com.synapsecore.config.SynapseCorsProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiCorsResponseFilter extends OncePerRequestFilter {

    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final List<String> DEFAULT_ALLOWED_HEADERS = List.of(
        "Content-Type",
        "X-Synapse-Tenant",
        "X-Request-Id",
        "Authorization"
    );

    private final SynapseCorsProperties corsProperties;

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isApiRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String allowedOrigin = resolveAllowedOrigin(request);
        if (allowedOrigin != null) {
            applyCorsHeaders(request, response, allowedOrigin);
            if (isPreflightRequest(request)) {
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri != null && requestUri.startsWith("/api/")) {
            return true;
        }

        if (requestUri != null && requestUri.equals("/error")) {
            Object originalUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
            return originalUri instanceof String uri && uri.startsWith("/api/");
        }

        return false;
    }

    private String resolveAllowedOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isBlank()) {
            return null;
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
        configuration.setAllowCredentials(true);
        return configuration.checkOrigin(origin);
    }

    private void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response, String allowedOrigin) {
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, String.join(", ", ALLOWED_METHODS));
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, resolveAllowedHeaders(request));
        response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    }

    private String resolveAllowedHeaders(HttpServletRequest request) {
        String requestedHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        if (requestedHeaders != null && !requestedHeaders.isBlank()) {
            return requestedHeaders;
        }
        return String.join(", ", DEFAULT_ALLOWED_HEADERS);
    }

    private boolean isPreflightRequest(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
            && request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD) != null;
    }
}
