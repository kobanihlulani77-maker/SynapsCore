package com.synapsecore.integration;

import com.synapsecore.access.AccessDirectoryService;
import com.synapsecore.audit.AuditLogService;
import com.synapsecore.auth.StarterAccessUsers;
import com.synapsecore.config.SynapseStarterProperties;
import com.synapsecore.domain.entity.AccessOperator;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.IntegrationConnector;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationImportStatus;
import com.synapsecore.domain.entity.IntegrationInboundStatus;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.domain.entity.IntegrationReplayRecord;
import com.synapsecore.domain.entity.IntegrationSyncMode;
import com.synapsecore.domain.entity.IntegrationTransformationPolicy;
import com.synapsecore.domain.entity.IntegrationValidationPolicy;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.IntegrationImportRunRepository;
import com.synapsecore.domain.repository.IntegrationInboundRecordRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.WarehouseRepository;
import com.synapsecore.event.OperationalStateChangePublisher;
import com.synapsecore.event.OperationalUpdateType;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.integration.dto.IntegrationConnectorHealthStatus;
import com.synapsecore.integration.dto.IntegrationConnectorRequest;
import com.synapsecore.integration.dto.IntegrationConnectorResponse;
import java.time.Duration;
import java.time.Instant;
import com.synapsecore.tenant.TenantContextService;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.hibernate.LazyInitializationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class IntegrationConnectorService {

    private static final int SUPPORTED_MAPPING_VERSION = 1;

    private final IntegrationConnectorRepository integrationConnectorRepository;
    private final AccessOperatorRepository accessOperatorRepository;
    private final AccessDirectoryService accessDirectoryService;
    private final IntegrationInboundRecordRepository integrationInboundRecordRepository;
    private final IntegrationImportRunRepository integrationImportRunRepository;
    private final IntegrationReplayRecordRepository integrationReplayRecordRepository;
    private final BusinessEventService businessEventService;
    private final AuditLogService auditLogService;
    private final OperationalStateChangePublisher operationalStateChangePublisher;
    private final TenantContextService tenantContextService;
    private final SynapseStarterProperties starterProperties;
    private final WarehouseRepository warehouseRepository;

    @org.springframework.beans.factory.annotation.Value("${synapsecore.integration.health-window-hours:24}")
    private long integrationHealthWindowHours;

    @org.springframework.beans.factory.annotation.Value("${synapsecore.integration.pull-worker.allow-local-targets:false}")
    private boolean allowLocalPullTargets;

    @Transactional(readOnly = true)
    public List<IntegrationConnectorResponse> getConnectors() {
        return getConnectors(null, null);
    }

    @Transactional(readOnly = true)
    public List<IntegrationConnectorResponse> getConnectors(String sourceSystem, IntegrationConnectorType type) {
        var currentOperator = accessDirectoryService.getCurrentOperator();
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        String normalizedSourceSystem = normalizeOptional(sourceSystem) == null
            ? null
            : normalizeSourceSystem(sourceSystem);
        boolean exactReadback = normalizedSourceSystem != null && type != null;
        return resolveConnectorSelection(tenantCode, normalizedSourceSystem, type)
            .stream()
            .filter(connector -> currentOperator.isEmpty()
                || isConnectorVisibleTo(currentOperator.get(), connector))
            .map(connector -> exactReadback
                ? describeConnectorForExactReadback(connector)
                : describeConnector(connector))
            .toList();
    }

    private boolean isConnectorVisibleTo(AccessOperator operator, IntegrationConnector connector) {
        if (operator.getWarehouseScopes() == null || operator.getWarehouseScopes().isEmpty()) {
            return true;
        }
        String defaultWarehouseCode = normalizeOptional(connector.getDefaultWarehouseCode());
        return defaultWarehouseCode != null
            && accessDirectoryService.hasWarehouseAccess(operator, defaultWarehouseCode);
    }

    @Transactional
    public IntegrationConnectorResponse upsertConnector(IntegrationConnectorRequest request, String actorName) {
        var tenant = tenantContextService.getCurrentTenantOrDefault();
        String normalizedSourceSystem = normalizeSourceSystem(request.sourceSystem());
        if (request.defaultWarehouseCode() != null && !request.defaultWarehouseCode().isBlank()) {
            warehouseRepository.findByTenant_CodeIgnoreCaseAndCode(tenant.getCode(), request.defaultWarehouseCode().trim())
                .filter(com.synapsecore.domain.entity.Warehouse::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                    "Default warehouse must be an active warehouse in the current tenant."));
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
        String pullEndpointUrl = normalizeOptional(
            request.pullEndpointUrl() != null ? request.pullEndpointUrl() : connector.getPullEndpointUrl()
        );
        requireSupportedSyncMode(request.type(), syncMode, pullEndpointUrl);
        connector.setSyncIntervalMinutes(resolveSyncIntervalMinutes(syncMode, request.syncIntervalMinutes(), connector.getSyncIntervalMinutes()));
        connector.setPullEndpointUrl(syncMode == IntegrationSyncMode.SCHEDULED_PULL ? pullEndpointUrl : null);
        connector.setValidationPolicy(request.validationPolicy() != null
            ? request.validationPolicy()
            : connector.getId() == null ? defaultValidationPolicy(request.type()) : connector.getValidationPolicy());
        connector.setTransformationPolicy(request.transformationPolicy() != null
            ? request.transformationPolicy()
            : connector.getId() == null ? defaultTransformationPolicy() : connector.getTransformationPolicy());
        connector.setMappingVersion(resolveSupportedMappingVersion(request.mappingVersion(), connector.getMappingVersion()));
        connector.setAllowDefaultWarehouseFallback(request.allowDefaultWarehouseFallback() != null
            ? request.allowDefaultWarehouseFallback()
            : connector.getId() == null ? defaultAllowDefaultWarehouseFallback(request.type()) : connector.isAllowDefaultWarehouseFallback());
        connector.setDefaultWarehouseCode(normalizeOptional(request.defaultWarehouseCode()));
        connector.setNotes(normalizeOptional(request.notes()));
        applyInboundAccessTokenSettings(connector, request);
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

        return describeConnector(connector);
    }

    public IntegrationConnector requireEnabledConnector(String sourceSystem,
                                                        IntegrationConnectorType type,
                                                        String actionDescription) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        return requireEnabledConnectorForTenant(tenantCode, sourceSystem, type, actionDescription);
    }

    public IntegrationConnector requireConfiguredConnector(String sourceSystem,
                                                           IntegrationConnectorType type,
                                                           String actionDescription) {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        return requireConfiguredConnectorForTenant(tenantCode, sourceSystem, type, actionDescription);
    }

    public IntegrationConnector requireConfiguredConnectorForTenant(String tenantCode,
                                                                    String sourceSystem,
                                                                    IntegrationConnectorType type,
                                                                    String actionDescription) {
        String normalizedSourceSystem = normalizeSourceSystem(sourceSystem);
        return integrationConnectorRepository
            .findByTenantCodeAndSourceSystemAndTypeWithTenant(tenantCode, normalizedSourceSystem, type)
            .orElseThrow(() -> IntegrationFailureCodes.status(HttpStatus.NOT_FOUND,
                IntegrationFailureCode.CONNECTOR_NOT_CONFIGURED,
                "Integration connector not configured for sourceSystem " + normalizedSourceSystem + " and type " + type));
    }

    public IntegrationConnector requireEnabledConnectorForTenant(String tenantCode,
                                                                 String sourceSystem,
                                                                 IntegrationConnectorType type,
                                                                 String actionDescription) {
        return requireEnabled(
            requireConfiguredConnectorForTenant(tenantCode, sourceSystem, type, actionDescription),
            actionDescription
        );
    }

    public IntegrationConnector requireEnabledConnector(IntegrationConnector connector,
                                                        String actionDescription) {
        return requireEnabled(connector, actionDescription);
    }

    public IntegrationConnector requireEnabledConnectorByInboundToken(String sourceSystem,
                                                                      IntegrationConnectorType type,
                                                                      String inboundAccessToken,
                                                                      String actionDescription) {
        String normalizedSourceSystem = normalizeSourceSystem(sourceSystem);
        String tokenHash = hashInboundAccessToken(inboundAccessToken);
        IntegrationConnector connector = integrationConnectorRepository
            .findBySourceSystemIgnoreCaseAndTypeAndInboundAccessTokenHashWithTenant(normalizedSourceSystem, type, tokenHash)
            .orElseThrow(() -> IntegrationFailureCodes.status(HttpStatus.UNAUTHORIZED,
                IntegrationFailureCode.INVALID_CONNECTOR_TOKEN,
                "Connector token is invalid for sourceSystem " + normalizedSourceSystem + " and type " + type + "."));
        return requireEnabled(connector, actionDescription);
    }

    public String resolveTenantCode(IntegrationConnector connector) {
        if (connector == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Integration connector context is required to resolve tenant ownership.");
        }

        String tenantCode = extractTenantCode(connector);
        if (tenantCode != null) {
            return tenantCode;
        }

        if (connector.getId() != null) {
            IntegrationConnector hydratedConnector = integrationConnectorRepository.findByIdWithTenant(connector.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Integration connector not found for id " + connector.getId()));
            tenantCode = extractTenantCode(hydratedConnector);
            if (tenantCode != null) {
                return tenantCode;
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Connector " + connector.getSourceSystem() + " is missing tenant ownership.");
    }

    @Transactional
    public void seedStarterConnectors() {
        if (!starterProperties.isSeedStarterConnectorsOnTenantOnboarding()) {
            return;
        }
        seedStarterConnectors(tenantContextService.getCurrentTenantOrDefault());
    }

    @Transactional
    public void seedStarterConnectors(com.synapsecore.domain.entity.Tenant tenant) {
        if (!starterProperties.isSeedStarterConnectorsOnTenantOnboarding()) {
            return;
        }
        boolean starterTenant = StarterAccessUsers.STARTER_TENANT_CODE.equalsIgnoreCase(tenant.getCode());
        String sourcePrefix = starterTenant
            ? "erp"
            : tenant.getCode().trim().toLowerCase(Locale.ROOT).replace('-', '_');
        String displayPrefix = starterTenant ? "ERP" : tenant.getName();

        ensureConnector(tenant.getCode(), tenant, sourcePrefix + "_north", IntegrationConnectorType.WEBHOOK_ORDER,
            displayPrefix + " North Webhook", true, "WH-NORTH",
            "Starter webhook connector for inbound ERP orders.", "Operations Lead",
            IntegrationSyncMode.REALTIME_PUSH, IntegrationValidationPolicy.STANDARD, IntegrationTransformationPolicy.NORMALIZE_CODES, true);
        ensureConnector(tenant.getCode(), tenant, sourcePrefix + "_coast", IntegrationConnectorType.WEBHOOK_ORDER,
            displayPrefix + " Coast Webhook", true, "WH-COAST",
            "Starter webhook connector for coast operations.", "Operations Lead",
            IntegrationSyncMode.REALTIME_PUSH, IntegrationValidationPolicy.STANDARD, IntegrationTransformationPolicy.NORMALIZE_CODES, true);
        ensureConnector(tenant.getCode(), tenant, sourcePrefix + "_batch", IntegrationConnectorType.CSV_ORDER_IMPORT,
            displayPrefix + " Batch CSV Feed", true, null,
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
                if (existing.getMappingVersion() == null || existing.getMappingVersion() < 1) {
                    existing.setMappingVersion(1);
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
                .mappingVersion(1)
                .allowDefaultWarehouseFallback(allowDefaultWarehouseFallback)
                .defaultWarehouseCode(defaultWarehouseCode)
                .notes(notes)
                .supportOwnerActorName(supportOwnerActorName)
                .build()));
    }

    private String normalizeSourceSystem(String sourceSystem) {
        if (sourceSystem == null || sourceSystem.isBlank()) {
            throw IntegrationFailureCodes.badRequest(IntegrationFailureCode.INVALID_SOURCE_SYSTEM, "sourceSystem is required");
        }
        return sourceSystem.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<IntegrationConnector> resolveConnectorSelection(String tenantCode,
                                                                 String normalizedSourceSystem,
                                                                 IntegrationConnectorType type) {
        if (normalizedSourceSystem != null && type != null) {
            return integrationConnectorRepository
                .findByTenant_CodeIgnoreCaseAndSourceSystemIgnoreCaseAndType(tenantCode, normalizedSourceSystem, type)
                .stream()
                .toList();
        }
        if (normalizedSourceSystem != null) {
            return integrationConnectorRepository
                .findAllByTenant_CodeIgnoreCaseAndSourceSystemIgnoreCaseOrderByTypeAscSourceSystemAsc(
                    tenantCode,
                    normalizedSourceSystem
                );
        }

        List<IntegrationConnector> connectors = integrationConnectorRepository
            .findAllByTenant_CodeIgnoreCaseOrderByTypeAscSourceSystemAsc(tenantCode);
        if (type == null) {
            return connectors;
        }
        return connectors.stream()
            .filter(connector -> connector.getType() == type)
            .toList();
    }

    private String extractTenantCode(IntegrationConnector connector) {
        try {
            return connector.getTenant() == null ? null : normalizeOptional(connector.getTenant().getCode());
        } catch (LazyInitializationException exception) {
            return null;
        }
    }

    public static String hashInboundAccessToken(String inboundAccessToken) {
        if (inboundAccessToken == null || inboundAccessToken.isBlank()) {
            throw IntegrationFailureCodes.badRequest(IntegrationFailureCode.INVALID_CONNECTOR_TOKEN,
                "inboundAccessToken must be non-empty when configuring connector-authenticated ingress.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(inboundAccessToken.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for inbound connector token hashing.", exception);
        }
    }

    private void applyInboundAccessTokenSettings(IntegrationConnector connector,
                                                 IntegrationConnectorRequest request) {
        if (Boolean.TRUE.equals(request.clearInboundAccessToken())) {
            connector.setInboundAccessTokenHash(null);
            connector.setInboundAccessTokenHint(null);
            return;
        }
        if (request.inboundAccessToken() == null || request.inboundAccessToken().isBlank()) {
            return;
        }
        String trimmedToken = request.inboundAccessToken().trim();
        String tokenHash = hashInboundAccessToken(trimmedToken);
        boolean alreadyAssigned = integrationConnectorRepository.findAllByInboundAccessTokenHash(tokenHash).stream()
            .anyMatch(existing -> existing.getId() == null || !existing.getId().equals(connector.getId()));
        if (alreadyAssigned) {
            throw IntegrationFailureCodes.badRequest(
                IntegrationFailureCode.INVALID_CONNECTOR_TOKEN,
                "Inbound connector token is already assigned to another connector."
            );
        }
        connector.setInboundAccessTokenHash(tokenHash);
        connector.setInboundAccessTokenHint(maskInboundAccessToken(trimmedToken));
    }

    private String maskInboundAccessToken(String inboundAccessToken) {
        String trimmedToken = inboundAccessToken.trim();
        if (trimmedToken.length() <= 4) {
            return "****";
        }
        return "••••" + trimmedToken.substring(trimmedToken.length() - 4);
    }

    private IntegrationConnector requireEnabled(IntegrationConnector connector, String actionDescription) {
        if (!connector.isEnabled()) {
            throw IntegrationFailureCodes.status(HttpStatus.FORBIDDEN,
                IntegrationFailureCode.CONNECTOR_DISABLED,
                "Integration connector " + connector.getSourceSystem() + " is disabled and cannot " + actionDescription);
        }
        return connector;
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
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("https") && !scheme.equals("http")) || uri.getHost() == null || uri.getHost().isBlank()
                || uri.getUserInfo() != null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "pullEndpointUrl must be an absolute HTTP(S) URL without embedded credentials for scheduled pull connectors."
                );
            }
            requireSafePullTarget(uri);
        } catch (URISyntaxException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "pullEndpointUrl must be a valid HTTP(S) URL for scheduled pull connectors."
            );
        }
    }

    public void requireSafePullTarget(URI uri) {
        if (allowLocalPullTargets) {
            return;
        }
        String host = uri.getHost();
        if (host == null || host.isBlank() || host.equalsIgnoreCase("localhost")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scheduled pull endpoints cannot target localhost or an unresolved host.");
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isUnsafeAddress(address)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Scheduled pull endpoints cannot target private, loopback, link-local, multicast, or metadata network addresses.");
                }
            }
        } catch (UnknownHostException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Scheduled pull endpoint host could not be resolved safely.");
        }
    }

    private boolean isUnsafeAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        boolean uniqueLocalIpv6 = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
        return address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isMulticastAddress()
            || uniqueLocalIpv6;
    }

    private Integer resolveSupportedMappingVersion(Integer requestedMappingVersion, Integer existingMappingVersion) {
        int resolved = requestedMappingVersion != null
            ? requestedMappingVersion
            : existingMappingVersion == null || existingMappingVersion < 1 ? SUPPORTED_MAPPING_VERSION : existingMappingVersion;
        if (resolved != SUPPORTED_MAPPING_VERSION) {
            throw IntegrationFailureCodes.badRequest(
                IntegrationFailureCode.UNSUPPORTED_MAPPING_VERSION,
                "Only mappingVersion " + SUPPORTED_MAPPING_VERSION + " is currently supported for integration connectors."
            );
        }
        return resolved;
    }

    public IntegrationConnectorResponse describeConnector(IntegrationConnector connector) {
        AccessOperator supportOwner = resolveSupportOwner(connector);
        ConnectorTelemetry telemetry = buildTelemetry(connector);
        return toConnectorResponse(connector, supportOwner, telemetry);
    }

    private IntegrationConnectorResponse describeConnectorForExactReadback(IntegrationConnector connector) {
        return toConnectorResponse(connector, null, buildExactReadbackTelemetry(connector));
    }

    private IntegrationConnectorResponse toConnectorResponse(IntegrationConnector connector,
                                                             AccessOperator supportOwner,
                                                             ConnectorTelemetry telemetry) {
        return new IntegrationConnectorResponse(
            connector.getId(),
            connector.getVersion(),
            connector.getTenant() == null ? null : connector.getTenant().getCode(),
            connector.getSourceSystem(),
            connector.getType(),
            connector.getDisplayName(),
            connector.isEnabled(),
            connector.getSyncMode(),
            supportedSyncModesFor(connector.getType()),
            connector.getSyncIntervalMinutes(),
            connector.getPullEndpointUrl(),
            connector.getLastPullAttemptAt(),
            connector.getLastPullSuccessAt(),
            connector.getLastPullStatus(),
            connector.getLastPullMessage(),
            connector.getValidationPolicy(),
            connector.getTransformationPolicy(),
            connector.getMappingVersion() == null || connector.getMappingVersion() < 1 ? 1 : connector.getMappingVersion(),
            connector.isAllowDefaultWarehouseFallback(),
            connector.getDefaultWarehouseCode(),
            connector.getNotes(),
            connector.getSupportOwnerActorName(),
            supportOwner == null ? null : supportOwner.getDisplayName(),
            connector.getInboundAccessTokenHash() != null && !connector.getInboundAccessTokenHash().isBlank(),
            connector.getInboundAccessTokenHint(),
            telemetry.healthStatus(),
            telemetry.healthSummary(),
            supportBoundaryFor(connector.getType()),
            telemetry.lastActivityAt(),
            telemetry.lastSuccessfulActivityAt(),
            telemetry.lastImportStatus(),
            telemetry.lastImportAt(),
            telemetry.recentInboundFailureCount(),
            telemetry.pendingReplayCount(),
            telemetry.deadLetterCount(),
            telemetry.lastFailureCode(),
            telemetry.lastFailureMessage(),
            telemetry.lastFailureAt(),
            telemetry.oldestPendingReplayAt(),
            telemetry.oldestPendingReplayAgeSeconds(),
            connector.getCreatedAt(),
            connector.getUpdatedAt()
        );
    }

    private List<IntegrationSyncMode> supportedSyncModesFor(IntegrationConnectorType type) {
        return switch (type) {
            case WEBHOOK_ORDER -> List.of(IntegrationSyncMode.REALTIME_PUSH, IntegrationSyncMode.SCHEDULED_PULL);
            case CSV_ORDER_IMPORT -> List.of(IntegrationSyncMode.BATCH_FILE_DROP);
        };
    }

    private String supportBoundaryFor(IntegrationConnectorType type) {
        return switch (type) {
            case WEBHOOK_ORDER -> "Webhook order feeds support realtime push and scheduled pull. Scheduled pull is limited to HTTP(S) order endpoints with a minimum 15 minute cadence.";
            case CSV_ORDER_IMPORT -> "CSV order import is implemented as batch file-drop intake only. Scheduled pull and realtime push are not supported for CSV connectors.";
        };
    }

    private AccessOperator resolveSupportOwner(IntegrationConnector connector) {
        if (connector.getTenant() == null
            || connector.getSupportOwnerActorName() == null
            || connector.getSupportOwnerActorName().isBlank()) {
            return null;
        }
        return accessOperatorRepository.findByTenant_CodeIgnoreCaseAndActorNameIgnoreCase(
                connector.getTenant().getCode(),
                connector.getSupportOwnerActorName()
            )
            .orElse(null);
    }

    private ConnectorTelemetry buildTelemetry(IntegrationConnector connector) {
        if (connector.getTenant() == null || connector.getTenant().getCode() == null) {
            return new ConnectorTelemetry(
                connector.isEnabled() ? IntegrationConnectorHealthStatus.LIVE : IntegrationConnectorHealthStatus.OFFLINE,
                connector.isEnabled()
                    ? "Connector is enabled but tenant telemetry is unavailable."
                    : "Connector is disabled and cannot ingest live activity.",
                null,
                null,
                null,
                null,
                0,
                0,
                0,
                null,
                null,
                null,
                null,
                null
            );
        }

        String tenantCode = connector.getTenant().getCode();
        Instant windowStart = Instant.now().minus(Duration.ofHours(Math.max(integrationHealthWindowHours, 1)));

        var lastActivity = integrationInboundRecordRepository
            .findTopByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeOrderByCreatedAtDesc(
                tenantCode,
                connector.getSourceSystem(),
                connector.getType()
            )
            .orElse(null);
        var lastSuccessfulActivity = integrationInboundRecordRepository
            .findTopByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeAndStatusInOrderByCreatedAtDesc(
                tenantCode,
                connector.getSourceSystem(),
                connector.getType(),
                List.of(IntegrationInboundStatus.ACCEPTED, IntegrationInboundStatus.REPLAYED)
            )
            .orElse(null);
        var lastImportRun = integrationImportRunRepository
            .findTopByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeOrderByCreatedAtDesc(
                tenantCode,
                connector.getSourceSystem(),
                connector.getType()
            )
            .orElse(null);
        var latestInboundFailure = integrationInboundRecordRepository
            .findTopByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeAndStatusInOrderByUpdatedAtDesc(
                tenantCode,
                connector.getSourceSystem(),
                connector.getType(),
                List.of(IntegrationInboundStatus.REJECTED, IntegrationInboundStatus.REPLAY_QUEUED)
            )
            .orElse(null);
        var latestReplayIssue = integrationReplayRecordRepository
            .findTopByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeAndStatusInOrderByUpdatedAtDesc(
                tenantCode,
                connector.getSourceSystem(),
                connector.getType(),
                List.of(IntegrationReplayStatus.REPLAY_FAILED, IntegrationReplayStatus.DEAD_LETTERED)
            )
            .orElse(null);
        var oldestPendingReplay = integrationReplayRecordRepository
            .findTopByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeAndStatusInOrderByCreatedAtAsc(
                tenantCode,
                connector.getSourceSystem(),
                connector.getType(),
                List.of(IntegrationReplayStatus.PENDING, IntegrationReplayStatus.REPLAY_FAILED)
            )
            .orElse(null);

        long recentInboundFailureCount = integrationInboundRecordRepository
            .countByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeAndStatusInAndCreatedAtAfter(
                tenantCode,
                connector.getSourceSystem(),
                connector.getType(),
                List.of(IntegrationInboundStatus.REJECTED, IntegrationInboundStatus.REPLAY_QUEUED),
                windowStart
            );
        long pendingReplayCount = integrationReplayRecordRepository
            .countByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeAndStatusIn(
                tenantCode,
                connector.getSourceSystem(),
                connector.getType(),
                List.of(IntegrationReplayStatus.PENDING, IntegrationReplayStatus.REPLAY_FAILED)
            );
        long deadLetterCount = integrationReplayRecordRepository
            .countByTenantCodeIgnoreCaseAndSourceSystemIgnoreCaseAndConnectorTypeAndStatusIn(
                tenantCode,
                connector.getSourceSystem(),
                connector.getType(),
                List.of(IntegrationReplayStatus.DEAD_LETTERED)
            );

        IntegrationConnectorHealthStatus healthStatus = resolveHealthStatus(
            connector,
            recentInboundFailureCount,
            pendingReplayCount,
            deadLetterCount,
            lastImportRun == null ? null : lastImportRun.getStatus()
        );
        FailureSignal latestFailureSignal = resolveLatestFailureSignal(latestInboundFailure, latestReplayIssue);
        Instant oldestPendingReplayAt = oldestPendingReplay == null ? null : oldestPendingReplay.getCreatedAt();
        Long oldestPendingReplayAgeSeconds = oldestPendingReplayAt == null
            ? null
            : Math.max(Duration.between(oldestPendingReplayAt, Instant.now()).getSeconds(), 0L);

        return new ConnectorTelemetry(
            healthStatus,
            buildHealthSummary(connector, healthStatus, recentInboundFailureCount, pendingReplayCount, deadLetterCount, lastActivity),
            lastActivity == null ? null : lastActivity.getCreatedAt(),
            lastSuccessfulActivity == null ? null : lastSuccessfulActivity.getCreatedAt(),
            lastImportRun == null ? null : lastImportRun.getStatus(),
            lastImportRun == null ? null : lastImportRun.getCreatedAt(),
            recentInboundFailureCount,
            pendingReplayCount,
            deadLetterCount,
            latestFailureSignal.failureCode(),
            latestFailureSignal.failureMessage(),
            latestFailureSignal.failureAt(),
            oldestPendingReplayAt,
            oldestPendingReplayAgeSeconds
        );
    }

    private ConnectorTelemetry buildExactReadbackTelemetry(IntegrationConnector connector) {
        if (!connector.isEnabled()) {
            return new ConnectorTelemetry(
                IntegrationConnectorHealthStatus.OFFLINE,
                "Connector is disabled and cannot ingest live activity.",
                null,
                null,
                null,
                null,
                0,
                0,
                0,
                null,
                null,
                null,
                null,
                null
            );
        }
        return new ConnectorTelemetry(
            IntegrationConnectorHealthStatus.DEGRADED,
            "Connector is enabled. Detailed health telemetry is deferred on exact readback so the hosted response stays deterministic under proof load.",
            null,
            null,
            null,
            null,
            0,
            0,
            0,
            null,
            null,
            null,
            null,
            null
        );
    }

    private IntegrationConnectorHealthStatus resolveHealthStatus(IntegrationConnector connector,
                                                                 long recentInboundFailureCount,
                                                                 long pendingReplayCount,
                                                                 long deadLetterCount,
                                                                 IntegrationImportStatus lastImportStatus) {
        if (!connector.isEnabled()) {
            return IntegrationConnectorHealthStatus.OFFLINE;
        }
        if (deadLetterCount > 0
            || pendingReplayCount > 0
            || recentInboundFailureCount > 0
            || lastImportStatus == IntegrationImportStatus.FAILURE
            || lastImportStatus == IntegrationImportStatus.PARTIAL_SUCCESS) {
            return IntegrationConnectorHealthStatus.DEGRADED;
        }
        return IntegrationConnectorHealthStatus.LIVE;
    }

    private String buildHealthSummary(IntegrationConnector connector,
                                      IntegrationConnectorHealthStatus healthStatus,
                                      long recentInboundFailureCount,
                                      long pendingReplayCount,
                                      long deadLetterCount,
                                      com.synapsecore.domain.entity.IntegrationInboundRecord lastActivity) {
        return switch (healthStatus) {
            case OFFLINE -> "Connector is disabled and cannot ingest live activity.";
            case DEGRADED -> "Connector is enabled but needs attention: "
                + recentInboundFailureCount + " recent inbound issue(s), "
                + pendingReplayCount + " replay item(s), "
                + deadLetterCount + " dead-lettered item(s).";
            case LIVE -> lastActivity == null
                ? "Connector is enabled and ready for live traffic."
                : "Connector is enabled and processing activity without recent integration failures.";
        };
    }

    private FailureSignal resolveLatestFailureSignal(com.synapsecore.domain.entity.IntegrationInboundRecord latestInboundFailure,
                                                     IntegrationReplayRecord latestReplayIssue) {
        Instant inboundFailureAt = latestInboundFailure == null ? null : latestInboundFailure.getUpdatedAt();
        Instant replayFailureAt = latestReplayIssue == null ? null : latestReplayIssue.getUpdatedAt();
        if (replayFailureAt != null && (inboundFailureAt == null || replayFailureAt.isAfter(inboundFailureAt))) {
            return new FailureSignal(
                latestReplayIssue.getFailureCode(),
                latestReplayIssue.getFailureMessage(),
                replayFailureAt
            );
        }
        if (latestInboundFailure != null) {
            return new FailureSignal(
                latestInboundFailure.getFailureCode(),
                latestInboundFailure.getFailureMessage(),
                inboundFailureAt
            );
        }
        return new FailureSignal(null, null, null);
    }

    private record ConnectorTelemetry(
        IntegrationConnectorHealthStatus healthStatus,
        String healthSummary,
        Instant lastActivityAt,
        Instant lastSuccessfulActivityAt,
        IntegrationImportStatus lastImportStatus,
        Instant lastImportAt,
        long recentInboundFailureCount,
        long pendingReplayCount,
        long deadLetterCount,
        IntegrationFailureCode lastFailureCode,
        String lastFailureMessage,
        Instant lastFailureAt,
        Instant oldestPendingReplayAt,
        Long oldestPendingReplayAgeSeconds
    ) {
    }

    private record FailureSignal(
        IntegrationFailureCode failureCode,
        String failureMessage,
        Instant failureAt
    ) {
    }
}
