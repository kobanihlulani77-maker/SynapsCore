package com.synapsecore.access;

import com.synapsecore.config.SynapseBootstrapProperties;
import com.synapsecore.domain.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BootstrapAccessService {

    public static final String BOOTSTRAP_TOKEN_HEADER = "X-Synapse-Bootstrap-Token";

    private final TenantRepository tenantRepository;
    private final SynapseBootstrapProperties bootstrapProperties;

    public boolean isInitialBootstrapAvailable() {
        return tenantRepository.count() == 0L;
    }

    public boolean isBootstrapRequest(HttpServletRequest request) {
        String providedToken = request.getHeader(BOOTSTRAP_TOKEN_HEADER);
        return providedToken != null && !providedToken.isBlank();
    }

    public void requireInitialBootstrap(HttpServletRequest request) {
        if (!isInitialBootstrapAvailable()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Initial tenant bootstrap is not available after the first tenant has been created.");
        }
        if (!bootstrapProperties.hasInitialToken()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Initial tenant bootstrap is disabled because no bootstrap token is configured.");
        }
        if (!hasMatchingBootstrapToken(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "A valid bootstrap token is required to create the first tenant.");
        }
    }

    private boolean hasMatchingBootstrapToken(HttpServletRequest request) {
        String providedToken = request.getHeader(BOOTSTRAP_TOKEN_HEADER);
        if (providedToken == null || providedToken.isBlank() || !bootstrapProperties.hasInitialToken()) {
            return false;
        }
        return providedToken.trim().equals(bootstrapProperties.getInitialToken().trim());
    }
}
