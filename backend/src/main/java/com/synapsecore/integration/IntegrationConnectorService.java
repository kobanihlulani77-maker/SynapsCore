package com.synapsecore.integration;

import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.audit.AuditLogService;
import com.synapsecore.auth.DemoAccessUsers;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.IntegrationConnector;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationSyncMode;
import com.synapsecore.domain.entity.IntegrationTransformationPolicy;
import com.synapsecore.domain.entity.IntegrationValidationPolicy;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.event.OperationalStateChangePublisher;
import com.synapsecore.event.OperationalUpdateType;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.integration.dto.IntegrationConnectorRequest;
import com.synapsecore.integration.dto.IntegrationConnectorResponse;
import com.synapsecore.tenant.TenantContextService;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class IntegrationConnectorService {

    private final IntegrationConnectorRepository integrationConnectorRepository;
    private final AccessOperatorRepository accessOperatorRepository;
    private final AccessDirectoryService accessDirectoryService;
    private final BusinessEventService businessEventService;
    private final AuditLogService auditLogService;
    private final OperationalStateChangePublisher operationalStateChangePublisher;
    private final TenantContextService tenantContextService;

    @Transactional(readOnly = true)
    public List<IntegrationConnectorResponse> getConnectors() {
        var currentOperator = accessDirectoryService.getCurrentOperator();
        return integrationConnectorRepository.findAllByTenant_CodeIgnoreCaseOrderByTypeAscSourceSystemAsc(
                tenantContextService.getCurrentTenantCodeOrDefault())
            .stream()
            .filter(connector -> currentOperator.isEmpty()
                || connector.getDefaultWarehouseCode() == null
                || connector.getDefaultWarehouseCode().isBlank()
                || accessDirectoryService.hasWarehouseAccess(currentOperator.get(), connector.getDefaultWarehouseCode()))
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public IntegrationConnectorResponse upsertConnector(IntegrationConnectorRequest request, String actorName) {
        var tenant = tenantContextService.getCurrentTenantOrDefault();
        String normalizedSourceSystem = normalizeSourceSystem(request.sourceSystem());
        if (request.defaultWarehouseCode() != null && !request.defaultWarehouseCode().isBlank()) {
            accessDirectoryService.requireOperatorWarehouseAccess(
                actorName,
                tenant.getCode(),
                request.defaultWarehouseCode(),
                "manage integration connectors for warehouse " + request.defaultWarehouseCode().trim()
            );
        }
        IntegrationConnector connector = integrationConnectorRepository
            .findByTenant_CodeIgnoreCaseAndSourceSystemIgnoreCaseAndType(tenant.getCode(), normalizedSourceSystem, request.type())
            .orElseGet(() -> IntegrationConnector.builder()
                .tenant(tenant)
                .sourceSystem(normalizedSourceSystem)
                .type(request.type())
                .build());

        connector.setDisplayName(request.displayName().trim());
        connector.setEnabled(request.enabled());
        IntegrationSyncMode syncMode = request.syncMode() != null
            ? request.syncMode()
            : connector.getId() == null ? defaultSyncMode(request.type()) : connector.getSyncMode();
        connector.setSyncMode(syncMode);
        connector.setSyncIntervalMinutes(resolveSyncIntervalMinutes(syncMode, request.syncIntervalMinutes(), connector.getSyncIntervalMinutes()));
        connector.setValidationPolicy(request.validationPolicy() != null
            ? request.validationPolicy()
            : connector.getId() == null ? defaultValidationPolicy(request.type()) : connector.getValidationPolicy());
        connector.setTransformationPolicy(request.transformationPolicy() != null
            ? request.transformationPolicy()
            : connector.getId() == null ? defaultTransformationPolicy() : connector.getTransformationPolicy());
        connector.setAllowDefaultWarehouseFallback(request.allowDefaultWarehouseFallback() != null
            ? request.allowDefaultWarehouseFallback()
            : connector.getId() == null ? defaultAllowDefaultWarehouseFallback(request.type()) : connector.isAllowDefaultWarehouseFallback());
        connector.setDefaultWarehouseCode(normalizeOptional(request.defaultWarehouseCode()));
        connector.setNotes(normalizeOptional(request.notes()));
        connector = integrationConnectorRepository.save(connector);

        businessEventService.record(
            BusinessEventType.INTEGRATION_CONNECTOR_UPDATED,
            "integration-admin",
            "Connector " + connector.getSourceSystem() + " (" + connector.getType() + ") updated by " + actorName
                + " with enabled=" + connector.isEnabled() + " and syncMode=" + connector.getSyncMode() + "."
        );
        auditLogService.recordSuccess(
            "INTEGRATION_CONNECTOR_UPDATED",
            actorName,
            "integration-admin",
            "IntegrationConnector",
            connector.getSourceSystem() + ":" + connector.getType(),
            "Connector " + connector.getDisplayName() + " saved with enabled=" + connector.isEnabled()
                + ", syncMode=" + connector.getSyncMode()
                + ", validationPolicy=" + connector.getValidationPolicy()
                + ", transformationPolicy=" + connector.getTransformationPolicy()
                + ", allowDefaultWarehouseFallback=" + connector.isAllowDefaultWarehouseFallback() + "."
        );
        operationalStateChangePublisher.publish(OperationalUpdateType.INTEGRATION_STATE, "integration-admin");

        return toResponse(connector);
    }

    public IntegrationConnector requireEnabledConnector(String sourceSystem,
                                                        IntegrationConnectorType type,
                                                        String actionDescription) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        String normalizedSourceSystem = normalizeSourceSystem(sourceSystem);
        IntegrationConnector connector = integrationConnectorRepository
            .findByTenant_CodeIgnoreCaseAndSourceSystemIgnoreCaseAndType(tenantCode, normalizedSourceSystem, type)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Integration connector not configured for sourceSystem " + normalizedSourceSystem + " and type " + type));

        if (!connector.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Integration connector " + normalizedSourceSystem + " is disabled and cannot " + actionDescription);
        }

        return connector;
    }

    @Transactional
    public void seedStarterConnectors() {
        seedStarterConnectors(tenantContextService.getCurrentTenantOrDefault());
    }

    @Transactional
    public void seedStarterConnectors(com.synapsecore.domain.entity.Tenant tenant) {
        boolean defaultTenant = DemoAccessUsers.DEFAULT_TENANT_CODE.equalsIgnoreCase(tenant.getCode());
        String tenantPrefix = tenant.getCode().trim().toLowerCase(Locale.ROOT).replace('-', '_');
        ensureConnector(tenant.getCode(), tenant, defaultTenant ? "erp_north" : tenantPrefix + "_north", IntegrationConnectorType.WEBHOOK_ORDER,
            defaultTenant ? "ERP North Webhook" : tenant.getName() + " North Webhook", true, "WH-NORTH",
            "Starter webhook connector for inbound ERP orders.", "Operations Lead",
            IntegrationSyncMode.REALTIME_PUSH, IntegrationValidationPolicy.STANDARD, IntegrationTransformationPolicy.NORMALIZE_CODES, true);
        ensureConnector(tenant.getCode(), tenant, defaultTenant ? "erp_coast" : tenantPrefix + "_coast", IntegrationConnectorType.WEBHOOK_ORDER,
            defaultTenant ? "ERP Coast Webhook" : tenant.getName() + " Coast Webhook", true, "WH-COAST",
            "Starter webhook connector for coast operations.", "Operations Lead",
            IntegrationSyncMode.REALTIME_PUSH, IntegrationValidationPolicy.STANDARD, IntegrationTransformationPolicy.NORMALIZE_CODES, true);
        ensureConnector(tenant.getCode(), tenant, defaultTenant ? "erp_batch" : tenantPrefix + "_batch", IntegrationConnectorType.CSV_ORDER_IMPORT,
            defaultTenant ? "ERP Batch CSV Feed" : tenant.getName() + " Batch CSV Feed", true, null,
            "Starter CSV batch connector for operational order drops.", "Operations Lead",
            IntegrationSyncMode.BATCH_FILE_DROP, IntegrationValidationPolicy.RELAXED, IntegrationTransformationPolicy.NORMALIZE_CODES, false);
    }

    private void ensureConnector(String tenantCode,
                                 com.synapsecore.domain.entity.Tenant tenant,
                                 String sourceSystem,
                                 IntegrationConnectorType type,
                                 String displayName,
                                 boolean enabled,
                                 String defaultWarehouseCode,
                                 String notes,
                                 String supportOwnerActorName,
                                 IntegrationSyncMode syncMode,
                                 IntegrationValidationPolicy validationPolicy,
                                 IntegrationTransformationPolicy transformationPolicy,
                                 boolean allowDefaultWarehouseFallback) {
        integrationConnectorRepository.findByTenant_CodeIgnoreCaseAndSourceSystemIgnoreCaseAndType(tenantCode, sourceSystem, type)
            .map(existing -> {
                if (existing.getSupportOwnerActorName() == null || existing.getSupportOwnerActorName().isBlank()) {
                    existing.setSupportOwnerActorName(supportOwnerActorName);
                }
                if (existing.getSyncMode() == null) {
                    existing.setSyncMode(syncMode);
                }
                if (existing.getValidationPolicy() == null) {
                    existing.setValidationPolicy(validationPolicy);
                }
                if (existing.getTransformationPolicy() == null) {
                    existing.setTransformationPolicy(transformationPolicy);
                }
                existing.setAllowDefaultWarehouseFallback(existing.isAllowDefaultWarehouseFallback() || allowDefaultWarehouseFallback);
                return integrationConnectorRepository.save(existing);
            })
            .orElseGet(() -> integrationConnectorRepository.save(IntegrationConnector.builder()
                .tenant(tenant)
                .sourceSystem(sourceSystem)
                .type(type)
                .displayName(displayName)
                .enabled(enabled)
                .syncMode(syncMode)
                .validationPolicy(validationPolicy)
                .transformationPolicy(transformationPolicy)
                .allowDefaultWarehouseFallback(allowDefaultWarehouseFallback)
                .defaultWarehouseCode(defaultWarehouseCode)
                .notes(notes)
                .supportOwnerActorName(supportOwnerActorName)
                .build()));
    }

    private String normalizeSourceSystem(String sourceSystem) {
        if (sourceSystem == null || sourceSystem.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sourceSystem is required");
        }
        return sourceSystem.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private IntegrationSyncMode defaultSyncMode(IntegrationConnectorType type) {
        return switch (type) {
            case WEBHOOK_ORDER -> IntegrationSyncMode.REALTIME_PUSH;
            case CSV_ORDER_IMPORT -> IntegrationSyncMode.BATCH_FILE_DROP;
        };
    }

    private IntegrationValidationPolicy defaultValidationPolicy(IntegrationConnectorType type) {
        return switch (type) {
            case WEBHOOK_ORDER -> IntegrationValidationPolicy.STANDARD;
            case CSV_ORDER_IMPORT -> IntegrationValidationPolicy.RELAXED;
        };
    }

    private IntegrationTransformationPolicy defaultTransformationPolicy() {
        return IntegrationTransformationPolicy.NORMALIZE_CODES;
    }

    private boolean defaultAllowDefaultWarehouseFallback(IntegrationConnectorType type) {
        return type == IntegrationConnectorType.WEBHOOK_ORDER;
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

    private IntegrationConnectorResponse toResponse(IntegrationConnector connector) {
        AccessOperator supportOwner = null;
        if (connector.getTenant() != null
            && connector.getSupportOwnerActorName() != null
            && !connector.getSupportOwnerActorName().isBlank()) {
            supportOwner = accessOperatorRepository.findByTenant_CodeIgnoreCaseAndActorNameIgnoreCase(
                    connector.getTenant().getCode(),
                    connector.getSupportOwnerActorName()
                )
                .orElse(null);
        }
        return new IntegrationConnectorResponse(
            connector.getId(),
            connector.getTenant() == null ? null : connector.getTenant().getCode(),
            connector.getSourceSystem(),
            connector.getType(),
            connector.getDisplayName(),
            connector.isEnabled(),
            connector.getSyncMode(),
            connector.getSyncIntervalMinutes(),
            connector.getValidationPolicy(),
            connector.getTransformationPolicy(),
            connector.isAllowDefaultWarehouseFallback(),
            connector.getDefaultWarehouseCode(),
            connector.getNotes(),
            connector.getSupportOwnerActorName(),
            supportOwner == null ? null : supportOwner.getDisplayName(),
            connector.getCreatedAt(),
            connector.getUpdatedAt()
        );
    }
}
