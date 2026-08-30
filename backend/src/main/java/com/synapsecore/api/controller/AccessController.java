package com.synapsecore.api.controller;

import com.synapsecore.access.AccessAdministrationService;
import com.synapsecore.access.AccessControlService;
import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.access.BootstrapAccessService;
import com.synapsecore.access.PlatformAdministrationAccessService;
import com.synapsecore.access.TenantWorkspaceAdministrationService;
import com.synapsecore.access.TenantOnboardingService;
import com.synapsecore.access.dto.AccessOperatorResponse;
import com.synapsecore.access.dto.AccessOperatorUpsertRequest;
import com.synapsecore.access.dto.AccessUserCreateRequest;
import com.synapsecore.access.dto.AccessUserPasswordResetRequest;
import com.synapsecore.access.dto.AccessUserResponse;
import com.synapsecore.access.dto.AccessUserUpdateRequest;
import com.synapsecore.access.dto.TenantWorkspaceConnectorSupportUpdateRequest;
import com.synapsecore.access.dto.TenantOnboardingRequest;
import com.synapsecore.access.dto.TenantOnboardingResponse;
import com.synapsecore.access.dto.TenantWorkspaceResponse;
import com.synapsecore.access.dto.TenantWorkspaceSecuritySettingsRequest;
import com.synapsecore.access.dto.TenantWorkspaceUpdateRequest;
import com.synapsecore.access.dto.TenantWorkspaceWarehouseUpdateRequest;
import com.synapsecore.access.dto.TenantWorkspaceWarehouseCreateRequest;
import com.synapsecore.access.dto.TenantWorkspaceWarehouseLifecycleRequest;
import com.synapsecore.access.dto.TenantResponse;
import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.config.SynapseCorsProperties;
import com.synapsecore.platform.PlatformOwnerSessionService;
import com.synapsecore.domain.dto.WarehouseResponse;
import com.synapsecore.integration.dto.IntegrationConnectorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/access")
@RequiredArgsConstructor
public class AccessController {

    private final AccessAdministrationService accessAdministrationService;
    private final AccessControlService accessControlService;
    private final AccessDirectoryService accessDirectoryService;
    private final BootstrapAccessService bootstrapAccessService;
    private final PlatformAdministrationAccessService platformAdministrationAccessService;
    private final TenantWorkspaceAdministrationService tenantWorkspaceAdministrationService;
    private final TenantOnboardingService tenantOnboardingService;
    private final AuthSessionService authSessionService;
    private final PlatformOwnerSessionService platformOwnerSessionService;
    private final SynapseCorsProperties corsProperties;

    @GetMapping("/tenants")
    public List<TenantResponse> getActiveTenants(HttpServletRequest httpRequest) {
        if (platformAdministrationAccessService.isPlatformAdminRequest(httpRequest)) {
            platformAdministrationAccessService.requirePlatformAdministration(httpRequest);
        } else if (bootstrapAccessService.isBootstrapRequest(httpRequest)) {
            bootstrapAccessService.requireInitialBootstrap(httpRequest);
        } else {
            platformOwnerSessionService.requirePlatformOwner(
                httpRequest.getSession(false),
                "view the global tenant directory"
            );
        }
        return accessDirectoryService.getActiveTenants();
    }

    @PostMapping("/tenants")
    public TenantOnboardingResponse onboardTenant(@Valid @RequestBody TenantOnboardingRequest request,
                                                  HttpServletRequest httpRequest) {
        String actorName;
        if (bootstrapAccessService.isInitialBootstrapAvailable()
                || bootstrapAccessService.isBootstrapRequest(httpRequest)) {
            bootstrapAccessService.requireInitialBootstrap(httpRequest);
            actorName = "platform-bootstrap";
        } else if (platformAdministrationAccessService.isPlatformAdminRequest(httpRequest)) {
            platformAdministrationAccessService.requirePlatformAdministration(httpRequest);
            actorName = "platform-admin";
        } else if (platformOwnerSessionService.hasSessionIdentity(httpRequest.getSession(false))) {
            requireTrustedCookieOrigin(httpRequest);
            actorName = platformOwnerSessionService.requirePlatformOwner(
                httpRequest.getSession(false),
                "create tenant workspaces"
            );
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Tenant workspace creation requires platform administration in this environment.");
        }
        return tenantOnboardingService.onboardTenant(request, actorName);
    }

    private void requireTrustedCookieOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isBlank()) {
            return;
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
        configuration.setAllowCredentials(true);
        if (configuration.checkOrigin(origin) == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Tenant provisioning requires a trusted application origin.");
        }
    }

    @GetMapping("/operators")
    public List<AccessOperatorResponse> getActiveOperators(@RequestParam(required = false) String tenantCode) {
        accessControlService.requireWorkspaceAccess("view workspace operators");
        return accessDirectoryService.getActiveOperators(tenantCode);
    }

    @GetMapping("/admin/operators")
    public List<AccessOperatorResponse> getTenantOperators() {
        accessControlService.requireTenantAdmin("view tenant access operators");
        return accessAdministrationService.getTenantOperators();
    }

    @PostMapping("/admin/operators")
    public AccessOperatorResponse createOperator(@Valid @RequestBody AccessOperatorUpsertRequest request) {
        String actorName = accessControlService.requireTenantAdmin("create tenant access operators").actorName();
        return accessAdministrationService.createOperator(request, actorName);
    }

    @PutMapping("/admin/operators/{operatorId}")
    public AccessOperatorResponse updateOperator(@PathVariable Long operatorId,
                                                 @Valid @RequestBody AccessOperatorUpsertRequest request) {
        String actorName = accessControlService.requireTenantAdmin("update tenant access operators").actorName();
        return accessAdministrationService.updateOperator(operatorId, request, actorName);
    }

    @GetMapping("/admin/workspace")
    public TenantWorkspaceResponse getWorkspace() {
        accessControlService.requireTenantAdmin("view tenant workspace settings");
        return tenantWorkspaceAdministrationService.getWorkspace();
    }

    @PutMapping("/admin/workspace")
    public TenantWorkspaceResponse updateWorkspace(@Valid @RequestBody TenantWorkspaceUpdateRequest request) {
        String actorName = accessControlService.requireTenantAdmin("update tenant workspace settings").actorName();
        return tenantWorkspaceAdministrationService.updateWorkspace(request, actorName);
    }

    @PutMapping("/admin/workspace/security")
    public TenantWorkspaceResponse updateWorkspaceSecurity(@Valid @RequestBody TenantWorkspaceSecuritySettingsRequest request,
                                                           HttpServletRequest httpRequest) {
        String actorName = accessControlService.requireTenantAdmin("update tenant security settings").actorName();
        TenantWorkspaceResponse workspace = tenantWorkspaceAdministrationService.updateSecuritySettings(request, actorName);
        authSessionService.syncTenantSecurityPolicy(
            httpRequest.getSession(false),
            workspace.tenantCode(),
            workspace.securitySettings().securityPolicyVersion(),
            workspace.securitySettings().sessionTimeoutMinutes()
        );
        return workspace;
    }

    @PutMapping("/admin/workspace/warehouses/{warehouseId}")
    public WarehouseResponse updateWorkspaceWarehouse(@PathVariable Long warehouseId,
                                                      @Valid @RequestBody TenantWorkspaceWarehouseUpdateRequest request) {
        String actorName = accessControlService.requireTenantAdmin("update tenant workspace warehouses").actorName();
        return tenantWorkspaceAdministrationService.updateWarehouse(warehouseId, request, actorName);
    }

    @PostMapping("/admin/workspace/warehouses")
    public WarehouseResponse createWorkspaceWarehouse(@Valid @RequestBody TenantWorkspaceWarehouseCreateRequest request) {
        String actorName = accessControlService.requireTenantAdmin("create tenant workspace warehouses").actorName();
        return tenantWorkspaceAdministrationService.createWarehouse(request, actorName);
    }

    @PostMapping("/admin/workspace/warehouses/{warehouseId}/retire")
    public WarehouseResponse retireWorkspaceWarehouse(@PathVariable Long warehouseId,
                                                       @RequestBody(required = false) TenantWorkspaceWarehouseLifecycleRequest request) {
        String actorName = accessControlService.requireTenantAdmin("retire tenant workspace warehouses").actorName();
        return tenantWorkspaceAdministrationService.retireWarehouse(warehouseId,
            request == null ? new TenantWorkspaceWarehouseLifecycleRequest(null) : request, actorName);
    }

    @PostMapping("/admin/workspace/warehouses/{warehouseId}/reactivate")
    public WarehouseResponse reactivateWorkspaceWarehouse(@PathVariable Long warehouseId,
                                                          @RequestBody(required = false) TenantWorkspaceWarehouseLifecycleRequest request) {
        String actorName = accessControlService.requireTenantAdmin("reactivate tenant workspace warehouses").actorName();
        return tenantWorkspaceAdministrationService.reactivateWarehouse(warehouseId,
            request == null ? new TenantWorkspaceWarehouseLifecycleRequest(null) : request, actorName);
    }

    @PutMapping("/admin/workspace/connectors/{connectorId}")
    public IntegrationConnectorResponse updateWorkspaceConnectorSupport(@PathVariable Long connectorId,
                                                                       @Valid @RequestBody TenantWorkspaceConnectorSupportUpdateRequest request) {
        String actorName = accessControlService.requireTenantAdmin("update tenant connector support ownership").actorName();
        return tenantWorkspaceAdministrationService.updateConnectorSupport(connectorId, request, actorName);
    }

    @GetMapping("/admin/users")
    public List<AccessUserResponse> getTenantUsers() {
        accessControlService.requireTenantAdmin("view tenant user accounts");
        return accessAdministrationService.getTenantUsers();
    }

    @PostMapping("/admin/users")
    public AccessUserResponse createUser(@Valid @RequestBody AccessUserCreateRequest request) {
        String actorName = accessControlService.requireTenantAdmin("create tenant user accounts").actorName();
        return accessAdministrationService.createUser(request, actorName);
    }

    @PutMapping("/admin/users/{userId}")
    public AccessUserResponse updateUser(@PathVariable Long userId,
                                         @Valid @RequestBody AccessUserUpdateRequest request) {
        String actorName = accessControlService.requireTenantAdmin("update tenant user accounts").actorName();
        return accessAdministrationService.updateUser(userId, request, actorName);
    }

    @PostMapping("/admin/users/{userId}/reset-password")
    public AccessUserResponse resetUserPassword(@PathVariable Long userId,
                                                @Valid @RequestBody AccessUserPasswordResetRequest request) {
        String actorName = accessControlService.requireTenantAdmin("reset tenant user passwords").actorName();
        return accessAdministrationService.resetUserPassword(userId, request, actorName);
    }
}
