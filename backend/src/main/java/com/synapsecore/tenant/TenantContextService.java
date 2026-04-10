package com.synapsecore.tenant;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.auth.DemoAccessUsers;
import com.synapsecore.config.SynapseAccessProperties;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantContextService {

    private final TenantRepository tenantRepository;
    private final SynapseAccessProperties accessProperties;
    private final AuthSessionService authSessionService;
    private final RequestTraceContext requestTraceContext;

    public String getCurrentTenantCodeOrDefault() {
        String traceTenantCode = requestTraceContext.getCurrentTenant()
            .filter(tenantCode -> !tenantCode.isBlank())
            .filter(tenantCode -> !RequestTraceContext.DEFAULT_TENANT.equalsIgnoreCase(tenantCode))
            .orElse(null);
        if (traceTenantCode != null) {
            return traceTenantCode;
        }

        String tenantCode = resolveTenantCodeFromRequest();
        if (tenantCode != null) {
            return tenantCode;
        }
        return getDefaultTenantCode();
    }

    public String getDefaultTenantCode() {
        return tenantRepository.findByCodeIgnoreCase(DemoAccessUsers.DEFAULT_TENANT_CODE)
            .filter(Tenant::isActive)
            .map(Tenant::getCode)
            .orElseGet(() -> tenantRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .findFirst()
                .map(Tenant::getCode)
                .orElse(DemoAccessUsers.DEFAULT_TENANT_CODE));
    }

    public Tenant getCurrentTenantOrDefault() {
        String tenantCode = getCurrentTenantCodeOrDefault();
        return tenantRepository.findByCodeIgnoreCase(tenantCode)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Tenant not found for current context: " + tenantCode
            ));
    }

    public List<Tenant> getActiveTenants() {
        return tenantRepository.findAllByActiveTrueOrderByNameAsc();
    }

    private String resolveTenantCodeFromRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false);
        if (session != null && authSessionService.hasSessionIdentity(session)) {
            return authSessionService.resolveAuthenticatedSession(session)
                .map(authenticatedSession -> authenticatedSession.tenant().getCode())
                .orElse(null);
        }

        if (accessProperties.isAllowHeaderFallback()) {
            String tenantHeader = request.getHeader(AccessControlService.TENANT_HEADER);
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                return tenantHeader.trim();
            }
        }

        return null;
    }
}
