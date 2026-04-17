package com.synapsecore.access;

import com.synapsecore.config.SynapseBootstrapProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PlatformAdministrationAccessService {

    public static final String PLATFORM_ADMIN_TOKEN_HEADER = "X-Synapse-Platform-Admin-Token";

    private final SynapseBootstrapProperties bootstrapProperties;

    public boolean isPlatformAdminRequest(HttpServletRequest request) {
        String providedToken = request.getHeader(PLATFORM_ADMIN_TOKEN_HEADER);
        return providedToken != null && !providedToken.isBlank();
    }

    public void requirePlatformAdministration(HttpServletRequest request) {
        if (!bootstrapProperties.hasPlatformAdminToken()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Platform administration is disabled because no platform admin token is configured.");
        }
        if (!hasMatchingPlatformAdminToken(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "A valid platform admin token is required to create tenant workspaces in this environment.");
        }
    }

    private boolean hasMatchingPlatformAdminToken(HttpServletRequest request) {
        String providedToken = request.getHeader(PLATFORM_ADMIN_TOKEN_HEADER);
        if (providedToken == null || providedToken.isBlank() || !bootstrapProperties.hasPlatformAdminToken()) {
            return false;
        }
        return providedToken.trim().equals(bootstrapProperties.getPlatformAdminToken().trim());
    }
}
