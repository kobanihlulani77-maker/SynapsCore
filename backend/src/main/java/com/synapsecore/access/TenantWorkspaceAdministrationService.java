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
import com.synapsecore.audit.AuditLogService;
import com.synapsecore.domain.dto.SystemIncidentResponse;
import com.synapsecore.domain.dto.WarehouseResponse;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.AccessUser;
import com.synapsecore.domain.entity.AuditLog;
import com.synapsecore.domain.entity.IntegrationConnector;
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
import com.synapsecore.domain.repository.ScenarioRunRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.domain.service.SystemIncidentService;
import com.synapsecore.integration.IntegrationConnectorService;
import com.synapsecore.integration.dto.IntegrationConnectorResponse;
import com.synapsecore.tenant.TenantContextService;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final SystemIncidentService systemIncidentService;
    private final AuditLogService auditLogService;
    private final IntegrationConnectorService integrationConnectorService;

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
            tenant.getCreatedAt(),
            tenant.getUpdatedAt()
        );
    }

    @Transactional
    public TenantWorkspaceResponse updateWorkspace(TenantWorkspaceUpdateRequest request, String actorName) {
        Tenant tenant = tenantContextService.getCurrentTenantOrDefault();
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

        String supportOwnerActorName = normalizeOptional(request.supportOwnerActorName());
        if (supportOwnerActorName != null) {
            AccessOperator supportOwner = accessDirectoryService.requireActiveOperator(
                supportOwnerActorName,
                tenantCode,
                "own connector support lanes"
            );
            accessDirectoryService.requireWarehouseAccess(
                supportOwner,
                connector.getDefaultWarehouseCode(),
                "own connector support lanes"
            );
            connector.setSupportOwnerActorName(supportOwner.getActorName());
        } else {
            connector.setSupportOwnerActorName(null);
        }
        IntegrationSyncMode nextSyncMode = request.syncMode() != null
            ? request.syncMode()
            : connector.getSyncMode();
        connector.setSyncMode(nextSyncMode);
        connector.setSyncIntervalMinutes(resolveSyncIntervalMinutes(
            nextSyncMode,
            request.syncIntervalMinutes(),
            connector.getSyncIntervalMinutes()
        ));
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

    private WarehouseResponse toWarehouseResponse(Warehouse warehouse) {
        return new WarehouseResponse(
            warehouse.getId(),
            warehouse.getCode(),
            warehouse.getName(),
            warehouse.getLocation()
        );
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Integer resolveSyncIntervalMinutes(IntegrationSyncMode syncMode,
                                               Integer requestedSyncIntervalMinutes,
                                               Integer existingSyncIntervalMinutes) {
        if (syncMode != IntegrationSyncMode.SCHEDULED_PULL) {
            return null;
        }
        Integer resolved = requestedSyncIntervalMinutes != null ? requestedSyncIntervalMinutes : existingSyncIntervalMinutes;
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "syncIntervalMinutes is required for scheduled pull connectors.");
        }
        if (resolved < 15 || resolved > 1440) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "syncIntervalMinutes must be between 15 and 1440 for scheduled pull connectors.");
        }
        return resolved;
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
