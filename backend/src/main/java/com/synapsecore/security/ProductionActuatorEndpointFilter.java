package com.synapsecore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile("prod")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProductionActuatorEndpointFilter extends OncePerRequestFilter {

    private static final String ACTUATOR_PREFIX = "/actuator";
    private static final String HEALTH_PATH = "/actuator/health";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        if (isRestrictedActuatorRequest(requestUri)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRestrictedActuatorRequest(String requestUri) {
        if (requestUri == null || !requestUri.startsWith(ACTUATOR_PREFIX)) {
            return false;
        }

        return !requestUri.equals(HEALTH_PATH) && !requestUri.startsWith(HEALTH_PATH + "/");
    }
}
