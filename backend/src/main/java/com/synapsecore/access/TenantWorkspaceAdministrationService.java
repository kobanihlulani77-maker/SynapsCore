package com.synapsecore.access;

import com.synapsecore.access.dto.TenantWorkspaceConnectorSupportUpdateRequest;
import com.synapsecore.access.dto.TenantWorkspaceSupportActivity;
import com.synapsecore.access.dto.TenantWorkspaceSupportDiagnostics;
import com.synapsecore.access.dto.TenantWorkspaceResponse;
import com.synapsecore.access.dto.TenantWorkspaceSecuritySettings;
import com.synapsecore.access.dto.TenantWorkspaceSecuritySettingsRequest;
import com.synapsecore.access.dto.TenantWorkspaceSupportSummary;
import com.synapsecore.access.dto.TenantWorkspaceUpdateRequest;
import com.synapsecore.access.dto.TenantWorkspaceWarehouseUpdateRequest;
import com.synapsecore.access.dto.TenantWorkspaceWarehouseCreateRequest;
import com.synapsecore.access.dto.TenantWorkspaceWarehouseLifecycleRequest;
import com.synapsecore.access.dto.TenantWorkspaceReadiness;
import com.synapsecore.access.SynapseAccessRole;
import com.synapsecore.audit.AuditLogService;
import com.synapsecore.domain.dto.SystemIncidentResponse;
import com.synapsecore.domain.dto.WarehouseResponse;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.AccessUser;
import com.synapsecore.domain.entity.AuditLog;
import com.synapsecore.domain.entity.IntegrationConnector;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationSyncMode;
import com.synapsecore.domain.entity.IntegrationTransformationPolicy;
import com.synapsecore.domain.entity.IntegrationValidationPolicy;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.domain.entity.ScenarioApprovalStatus;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.domain.entity.Warehouse;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.AccessUserRepository;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.CustomerOrderRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.InventoryRepository;
import com.synapsecore.domain.repository.ScenarioRunRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.domain.service.SystemIncidentService;
import com.synapsecore.integration.IntegrationConnectorService;
import com.synapsecore.integration.dto.IntegrationConnectorResponse;
import com.synapsecore.tenant.TenantContextService;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantWorkspaceAdministrationService {

    private final TenantContextService tenantContextService;
    private final AccessDirectoryService accessDirectoryService;
    private final AccessOperatorRepository accessOperatorRepository;
    private final AccessUserRepository accessUserRepository;
    private final AuditLogRepository auditLogRepository;
    private final WarehouseRepository warehouseRepository;
    private final IntegrationConnectorRepository integrationConnectorRepository;
    private final IntegrationReplayRecordRepository integrationReplayRecordRepository;
    private final ScenarioRunRepository scenarioRunRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final FulfillmentTaskRepository fulfillmentTaskRepository;
    private final InventoryRepository inventoryRepository;
    private final SystemIncidentService systemIncidentService;
    private final AuditLogService auditLogService;
    private final IntegrationConnectorService integrationConnectorService;

    @Transactional(readOnly = true)
    public TenantWorkspaceResponse getWorkspace() {
        Tenant tenant = tenantContextService.getCurrentTenantOrDefault();
        String tenantCode = tenant.getCode();
        List<AccessUser> users = accessUserRepository.findAllByTenant_CodeIgnoreCaseOrderByFullNameAscUsernameAsc(tenantCode);
        List<WarehouseResponse> warehouses = warehouseRepository.findAllByTenant_CodeIgnoreCaseOrderByNameAsc(tenantCode).stream()
            .map(this::toWarehouseResponse)
            .toList();
        List<IntegrationConnectorResponse> connectors = integrationConnectorRepository
            .findAllByTenant_CodeIgnoreCaseOrderByTypeAscSourceSystemAsc(tenantCode).stream()
            .map(integrationConnectorService::describeConnector)
            .toList();
        List<SystemIncidentResponse> supportIncidents = systemIncidentService.getActiveIncidents();
        List<TenantWorkspaceSupportActivity> recentSupportActivity = auditLogRepository
            .findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(tenantCode).stream()
            .filter(this::isSupportAudit)
            .limit(8)
            .map(this::toSupportActivity)
            .toList();
        long disabledConnectorCount = connectors.stream().filter(connector -> !connector.enabled()).count();
        long activeUsersRequiringPasswordChange = users.stream()
            .filter(AccessUser::isActive)
            .filter(AccessUser::isPasswordChangeRequired)
            .count();
        long activeUsersPastPasswordRotation = users.stream()
            .filter(AccessUser::isActive)
            .filter(user -> isPasswordRotationOverdue(user, tenant))
            .count();
        long activeUsersBlockedByInactiveOperator = users.stream()
            .filter(AccessUser::isActive)
            .filter(user -> user.getOperator() == null || !user.getOperator().isActive())
            .count();
        long connectorsWithoutSupportOwner = connectors.stream()
            .filter(connector -> connector.supportOwnerActorName() == null || connector.supportOwnerActorName().isBlank())
            .count();
        long highSeverityIncidentCount = supportIncidents.stream()
            .filter(incident -> incident.severity() == com.synapsecore.domain.dto.SystemIncidentSeverity.HIGH
                || incident.severity() == com.synapsecore.domain.dto.SystemIncidentSeverity.CRITICAL)
            .count();
        Instant latestSupportAuditAt = recentSupportActivity.isEmpty() ? null : recentSupportActivity.getFirst().createdAt();

        return new TenantWorkspaceResponse(
            tenant.getId(),
            tenant.getCode(),
            tenant.getName(),
            tenant.getDescription(),
            tenant.isActive(),
            new TenantWorkspaceSecuritySettings(
                tenant.getPasswordRotationDays(),
                tenant.getSessionTimeoutMinutes(),
                tenant.getSecurityPolicyVersion()
            ),
            new TenantWorkspaceSupportSummary(
                warehouses.size(),
                accessOperatorRepository.countByTenant_CodeIgnoreCaseAndActiveTrue(tenantCode),
                accessOperatorRepository.countByTenant_CodeIgnoreCaseAndActiveFalse(tenantCode),
                accessUserRepository.countByTenant_CodeIgnoreCaseAndActiveTrue(tenantCode),
                accessUserRepository.countByTenant_CodeIgnoreCaseAndActiveFalse(tenantCode),
                connectors.size() - disabledConnectorCount,
                disabledConnectorCount,
                integrationReplayRecordRepository.countByTenantCodeIgnoreCaseAndStatusIn(
                    tenantCode,
                    List.of(IntegrationReplayStatus.PENDING, IntegrationReplayStatus.REPLAY_FAILED)
                ),
                scenarioRunRepository.countByTenant_CodeIgnoreCaseAndApprovalStatus(
                    tenantCode,
                    ScenarioApprovalStatus.PENDING_APPROVAL
                ),
                supportIncidents.size()
            ),
            new TenantWorkspaceSupportDiagnostics(
                activeUsersRequiringPasswordChange,
                activeUsersPastPasswordRotation,
                activeUsersBlockedByInactiveOperator,
                connectorsWithoutSupportOwner,
                highSeverityIncidentCount,
                latestSupportAuditAt
            ),
            supportIncidents,
            recentSupportActivity,
            warehouses,
            connectors,
            tenant.getRequiredRoles().stream().sorted(Comparator.comparing(Enum::name)).toList(),
            calculateReadiness(tenant, users),
            tenant.getVersion(),
            tenant.getCreatedAt(),
            tenant.getUpdatedAt()
        );
    }

    @Transactional
    public TenantWorkspaceResponse updateWorkspace(TenantWorkspaceUpdateRequest request, String actorName) {
        Tenant tenant = tenantContextService.getCurrentTenantOrDefault();
        requireCurrentVersion(request.version(), tenant.getVersion(), "workspace settings");
        tenant.setName(request.tenantName().trim());
        tenant.setDescription(normalizeOptional(request.description()));

        auditLogService.recordSuccess(
            "TENANT_WORKSPACE_UPDATED",
            actorName,
            "tenant-admin",
            "Tenant",
            tenant.getCode(),
            "Updated tenant workspace metadata for " + tenant.getCode() + "."
        );

        return getWorkspace();
    }

    @Transactional
    public TenantWorkspaceResponse updateSecuritySettings(TenantWorkspaceSecuritySettingsRequest request,
                                                          String actorName) {
        Tenant tenant = tenantContextService.getCurrentTenantOrDefault();
        requireCurrentVersion(request.version(), tenant.getVersion(), "security settings");
        tenant.setPasswordRotationDays(request.passwordRotationDays());
        tenant.setSessionTimeoutMinutes(request.sessionTimeoutMinutes());
        if (request.invalidateOtherSessions()) {
            tenant.setSecurityPolicyVersion(tenant.getSecurityPolicyVersion() + 1);
        }

        auditLogService.recordSuccess(
            "TENANT_SECURITY_SETTINGS_UPDATED",
            actorName,
            "tenant-admin",
            "Tenant",
            tenant.getCode(),
            "Updated tenant security settings for " + tenant.getCode()
                + " with passwordRotationDays=" + tenant.getPasswordRotationDays()
                + " and sessionTimeoutMinutes=" + tenant.getSessionTimeoutMinutes()
                + (request.invalidateOtherSessions() ? " while invalidating other active sessions." : ".")
        );

        return getWorkspace();
    }

    @Transactional
    public WarehouseResponse updateWarehouse(Long warehouseId,
                                            TenantWorkspaceWarehouseUpdateRequest request,
                                            String actorName) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        Warehouse warehouse = warehouseRepository.findByTenant_CodeIgnoreCaseAndId(tenantCode, warehouseId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Warehouse not found in current tenant: " + warehouseId));
        requireCurrentVersion(request.version(), warehouse.getVersion(), "warehouse settings");

        warehouse.setName(request.name().trim());
        warehouse.setLocation(request.location().trim());

        auditLogService.recordSuccess(
            "TENANT_WAREHOUSE_UPDATED",
            actorName,
            "tenant-admin",
            "Warehouse",
            warehouse.getCode(),
            "Updated warehouse " + warehouse.getCode() + " for tenant " + tenantCode + "."
        );

        return toWarehouseResponse(warehouse);
    }

    @Transactional
    public IntegrationConnectorResponse updateConnectorSupport(Long connectorId,
                                                              TenantWorkspaceConnectorSupportUpdateRequest request,
                                                              String actorName) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        IntegrationConnector connector = integrationConnectorRepository.findByTenant_CodeIgnoreCaseAndId(tenantCode, connectorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Connector not found in current tenant: " + connectorId));
        requireCurrentVersion(request.version(), connector.getVersion(), "connector support settings");

        String supportOwnerActorName = normalizeOptional(request.supportOwnerActorName());
        AccessOperator supportOwner = null;
        if (supportOwnerActorName != null) {
            supportOwner = accessDirectoryService.requireActiveOperator(
                supportOwnerActorName,
                tenantCode,
                "own connector support lanes"
            );
            accessDirectoryService.requireWarehouseAccess(
                supportOwner,
                connector.getDefaultWarehouseCode(),
                "own connector support lanes"
            );
        }
        IntegrationSyncMode nextSyncMode = request.syncMode() != null
            ? request.syncMode()
            : connector.getSyncMode();
        String pullEndpointUrl = normalizeOptional(
            request.pullEndpointUrl() != null ? request.pullEndpointUrl() : connector.getPullEndpointUrl()
        );
        requireSupportedSyncMode(connector.getType(), nextSyncMode, pullEndpointUrl);
        if (supportOwner != null) {
            connector.setSupportOwnerActorName(supportOwner.getActorName());
        } else {
            connector.setSupportOwnerActorName(null);
        }
        connector.setSyncMode(nextSyncMode);
        connector.setSyncIntervalMinutes(resolveSyncIntervalMinutes(
            nextSyncMode,
            request.syncIntervalMinutes(),
            connector.getSyncIntervalMinutes()
        ));
        connector.setPullEndpointUrl(nextSyncMode == IntegrationSyncMode.SCHEDULED_PULL ? pullEndpointUrl : null);
        connector.setValidationPolicy(request.validationPolicy() != null
            ? request.validationPolicy()
            : connector.getValidationPolicy());
        connector.setTransformationPolicy(request.transformationPolicy() != null
            ? request.transformationPolicy()
            : connector.getTransformationPolicy());
        if (request.allowDefaultWarehouseFallback() != null) {
            connector.setAllowDefaultWarehouseFallback(request.allowDefaultWarehouseFallback());
        }
        connector.setNotes(normalizeOptional(request.notes()));

        connector = integrationConnectorRepository.save(connector);

        auditLogService.recordSuccess(
            "TENANT_CONNECTOR_SUPPORT_UPDATED",
            actorName,
            "tenant-admin",
            "IntegrationConnector",
            connector.getSourceSystem() + ":" + connector.getType(),
            "Updated support owner for connector " + connector.getDisplayName() + "."
        );

        return integrationConnectorService.describeConnector(connector);
    }

    @Transactional
    public WarehouseResponse createWarehouse(TenantWorkspaceWarehouseCreateRequest request, String actorName) {
        Tenant tenant = tenantContextService.getCurrentTenantOrDefault();
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (warehouseRepository.findByTenant_CodeIgnoreCaseAndCode(tenant.getCode(), code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Warehouse code already exists in current tenant: " + code);
        }
        Warehouse warehouse;
        try {
            warehouse = warehouseRepository.saveAndFlush(Warehouse.builder()
                .tenant(tenant)
                .code(code)
                .name(request.name().trim())
                .location(request.location().trim())
                .active(true)
                .build());
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Warehouse code already exists in current tenant: " + code, exception);
        }
        auditLogService.recordSuccess(
            "TENANT_WAREHOUSE_CREATED", actorName, "tenant-admin", "Warehouse", code,
            "Created warehouse " + code + " for tenant " + tenant.getCode() + "."
        );
        return toWarehouseResponse(warehouse);
    }

    @Transactional
    public WarehouseResponse retireWarehouse(Long warehouseId, TenantWorkspaceWarehouseLifecycleRequest request,
                                             String actorName) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        Warehouse warehouse = requireWarehouse(tenantCode, warehouseId);
        requireCurrentVersion(request.version(), warehouse.getVersion(), "warehouse lifecycle");
        if (!warehouse.isActive()) {
            return toWarehouseResponse(warehouse);
        }
        if (warehouseRepository.countByTenant_CodeIgnoreCaseAndActiveTrue(tenantCode) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "The last active warehouse cannot be retired.");
        }
        String code = warehouse.getCode();
        if (accessOperatorRepository.findAllByTenant_CodeIgnoreCaseAndActiveTrueOrderByDisplayNameAsc(tenantCode).stream()
            .anyMatch(operator -> !accessDirectoryService.getWarehouseScopes(operator).isEmpty()
                && accessDirectoryService.getWarehouseScopes(operator).stream().anyMatch(code::equalsIgnoreCase))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Warehouse " + code + " has active scoped operators. Reassign or deactivate them before retirement.");
        }
        if (integrationConnectorRepository.existsByTenant_CodeIgnoreCaseAndEnabledTrueAndDefaultWarehouseCodeIgnoreCase(tenantCode, code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Warehouse " + code + " is the default lane for an enabled connector.");
        }
        if (inventoryRepository.existsByWarehouse_IdAndQuantityReservedGreaterThan(warehouseId, 0L)
            || inventoryRepository.existsByWarehouse_IdAndQuantityInboundGreaterThan(warehouseId, 0L)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Warehouse " + code + " has reserved or inbound inventory.");
        }
        if (customerOrderRepository.findAllByTenant_CodeIgnoreCaseAndWarehouse_CodeIgnoreCase(tenantCode, code).stream()
            .anyMatch(order -> !isTerminal(order.getStatus()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Warehouse " + code + " has active orders.");
        }
        if (fulfillmentTaskRepository.findAllByTenant_CodeIgnoreCaseAndStatusInOrderByUpdatedAtDesc(
                tenantCode, List.of(com.synapsecore.domain.entity.FulfillmentStatus.QUEUED,
                    com.synapsecore.domain.entity.FulfillmentStatus.PICKING,
                    com.synapsecore.domain.entity.FulfillmentStatus.PACKED,
                    com.synapsecore.domain.entity.FulfillmentStatus.DISPATCHED,
                    com.synapsecore.domain.entity.FulfillmentStatus.DELAYED,
                    com.synapsecore.domain.entity.FulfillmentStatus.EXCEPTION)).stream()
            .anyMatch(task -> task.getWarehouse() != null && code.equalsIgnoreCase(task.getWarehouse().getCode()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Warehouse " + code + " has active fulfillment work.");
        }
        if (scenarioRunRepository.existsByTenant_CodeIgnoreCaseAndWarehouseCodeIgnoreCaseAndApprovalStatusIn(
                tenantCode, code, List.of(ScenarioApprovalStatus.PENDING_APPROVAL))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Warehouse " + code + " has pending governed scenarios.");
        }
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
        auditLogService.recordSuccess(
            "TENANT_WAREHOUSE_RETIRED", actorName, "tenant-admin", "Warehouse", code,
            "Retired warehouse " + code + " for tenant " + tenantCode + "."
        );
        return toWarehouseResponse(warehouse);
    }

    @Transactional
    public WarehouseResponse reactivateWarehouse(Long warehouseId, TenantWorkspaceWarehouseLifecycleRequest request,
                                                 String actorName) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        Warehouse warehouse = requireWarehouse(tenantCode, warehouseId);
        requireCurrentVersion(request.version(), warehouse.getVersion(), "warehouse lifecycle");
        if (warehouse.isActive()) {
            return toWarehouseResponse(warehouse);
        }
        warehouse.setActive(true);
        warehouseRepository.save(warehouse);
        auditLogService.recordSuccess(
            "TENANT_WAREHOUSE_REACTIVATED", actorName, "tenant-admin", "Warehouse", warehouse.getCode(),
            "Reactivated warehouse " + warehouse.getCode() + " for tenant " + tenantCode + "."
        );
        return toWarehouseResponse(warehouse);
    }

    private Warehouse requireWarehouse(String tenantCode, Long warehouseId) {
        return warehouseRepository.findByTenant_CodeIgnoreCaseAndId(tenantCode, warehouseId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Warehouse not found in current tenant: " + warehouseId));
    }

    private boolean isTerminal(com.synapsecore.domain.entity.OrderStatus status) {
        return status == com.synapsecore.domain.entity.OrderStatus.CANCELLED
            || status == com.synapsecore.domain.entity.OrderStatus.FAILED
            || status == com.synapsecore.domain.entity.OrderStatus.RETURNED
            || status == com.synapsecore.domain.entity.OrderStatus.DELIVERED;
    }

    private TenantWorkspaceReadiness calculateReadiness(Tenant tenant, List<AccessUser> users) {
        List<String> reasons = new ArrayList<>();
        if (!tenant.isActive()) reasons.add("Tenant workspace is inactive.");
        if (warehouseRepository.countByTenant_CodeIgnoreCaseAndActiveTrue(tenant.getCode()) == 0) {
            reasons.add("At least one active warehouse is required.");
        }
        if (users.stream().noneMatch(this::isUsableTenantAdminUser)) {
            reasons.add("An active Tenant Admin user is required.");
        }
        List<Warehouse> activeWarehouses = warehouseRepository.findAllByTenant_CodeIgnoreCaseOrderByNameAsc(tenant.getCode()).stream()
            .filter(Warehouse::isActive).toList();
        List<AccessOperator> operators = accessOperatorRepository.findAllByTenant_CodeIgnoreCaseOrderByDisplayNameAsc(tenant.getCode());
        for (SynapseAccessRole role : tenant.getRequiredRoles()) {
            for (Warehouse warehouse : activeWarehouses) {
                boolean covered = operators.stream().anyMatch(operator -> operator.isActive()
                    && operator.getRoles().contains(role)
                    && (accessDirectoryService.getWarehouseScopes(operator).isEmpty()
                        || accessDirectoryService.getWarehouseScopes(operator).stream().anyMatch(warehouse.getCode()::equalsIgnoreCase))
                    && users.stream().anyMatch(user -> isUsableUserForOperator(user, operator)));
                if (!covered) reasons.add("Required " + role + " coverage is missing for " + warehouse.getCode() + ".");
            }
        }
        return new TenantWorkspaceReadiness(reasons.isEmpty(), reasons.stream().distinct().toList());
    }

    private boolean isUsableTenantAdminUser(AccessUser user) {
        return user.isActive() && user.getOperator() != null && user.getOperator().isActive()
            && user.getOperator().getRoles().contains(SynapseAccessRole.TENANT_ADMIN);
    }

    private boolean isUsableUserForOperator(AccessUser user, AccessOperator operator) {
        return user.isActive() && user.getOperator() != null && user.getOperator().getId().equals(operator.getId());
    }

    private WarehouseResponse toWarehouseResponse(Warehouse warehouse) {
        return new WarehouseResponse(
            warehouse.getId(),
            warehouse.getCode(),
            warehouse.getName(),
            warehouse.getLocation(),
            warehouse.isActive(),
            warehouse.getVersion()
        );
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void requireCurrentVersion(Long requestedVersion, Long currentVersion, String subject) {
        if (requestedVersion == null || currentVersion == null || !requestedVersion.equals(currentVersion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Administrative " + subject + " changed. Refresh and try again.");
        }
    }

    private Integer resolveSyncIntervalMinutes(IntegrationSyncMode syncMode,
                                               Integer requestedSyncIntervalMinutes,
                                               Integer existingSyncIntervalMinutes) {
        if (syncMode != IntegrationSyncMode.SCHEDULED_PULL) {
            return null;
        }
        if (requestedSyncIntervalMinutes != null) {
            return requestedSyncIntervalMinutes;
        }
        if (existingSyncIntervalMinutes != null && existingSyncIntervalMinutes >= 15) {
            return existingSyncIntervalMinutes;
        }
        return 15;
    }

    private void requireSupportedSyncMode(IntegrationConnectorType type,
                                          IntegrationSyncMode syncMode,
                                          String pullEndpointUrl) {
        if (syncMode != IntegrationSyncMode.SCHEDULED_PULL) {
            return;
        }
        if (type != IntegrationConnectorType.WEBHOOK_ORDER) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Scheduled pull is currently implemented only for order API feeds."
            );
        }
        if (pullEndpointUrl == null || pullEndpointUrl.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "pullEndpointUrl is required for scheduled pull connectors."
            );
        }
        try {
            URI uri = new URI(pullEndpointUrl);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(java.util.Locale.ROOT);
            if ((!scheme.equals("https") && !scheme.equals("http")) || uri.getHost() == null || uri.getHost().isBlank()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "pullEndpointUrl must be an absolute HTTP(S) URL for scheduled pull connectors."
                );
            }
        } catch (URISyntaxException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "pullEndpointUrl must be a valid HTTP(S) URL for scheduled pull connectors."
            );
        }
    }

    private boolean isPasswordRotationOverdue(AccessUser user, Tenant tenant) {
        Instant passwordUpdatedAt = user.getPasswordUpdatedAt();
        if (passwordUpdatedAt == null) {
            return true;
        }
        return Instant.now().isAfter(passwordUpdatedAt.plus(Duration.ofDays(tenant.getPasswordRotationDays())));
    }

    private boolean isSupportAudit(AuditLog auditLog) {
        String action = auditLog.getAction();
        return action != null
            && (action.startsWith("ACCESS_")
            || action.startsWith("TENANT_")
            || action.startsWith("AUTH_"));
    }

    private TenantWorkspaceSupportActivity toSupportActivity(AuditLog auditLog) {
        String action = auditLog.getAction();
        return new TenantWorkspaceSupportActivity(
            auditLog.getId(),
            resolveSupportCategory(action),
            action,
            buildSupportTitle(auditLog),
            auditLog.getActor(),
            auditLog.getStatus(),
            auditLog.getTargetRef(),
            auditLog.getDetails(),
            auditLog.getRequestId(),
            auditLog.getCreatedAt()
        );
    }

    private String resolveSupportCategory(String action) {
        if (action == null || action.isBlank()) {
            return "SUPPORT";
        }
        if (action.startsWith("AUTH_")) {
            return "SECURITY";
        }
        if (action.startsWith("ACCESS_")) {
            return "ACCESS";
        }
        if ("TENANT_SECURITY_SETTINGS_UPDATED".equals(action)) {
            return "SECURITY";
        }
        if ("TENANT_CONNECTOR_SUPPORT_UPDATED".equals(action)) {
            return "CONNECTOR_SUPPORT";
        }
        if ("TENANT_WAREHOUSE_UPDATED".equals(action)) {
            return "WAREHOUSE";
        }
        if ("TENANT_WORKSPACE_UPDATED".equals(action) || "TENANT_ONBOARDED".equals(action)) {
            return "WORKSPACE";
        }
        return "SUPPORT";
    }

    private String buildSupportTitle(AuditLog auditLog) {
        String action = auditLog.getAction();
        if (action == null || action.isBlank()) {
            return "Tenant support event";
        }
        String actionLabel = formatCodeLabel(action);
        if (auditLog.getTargetRef() == null || auditLog.getTargetRef().isBlank()) {
            return actionLabel;
        }
        return actionLabel + " • " + auditLog.getTargetRef();
    }

    private String formatCodeLabel(String value) {
        return String.join(" ",
            java.util.Arrays.stream(value.toLowerCase().replace('-', '_').split("_"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .toList());
    }
}
