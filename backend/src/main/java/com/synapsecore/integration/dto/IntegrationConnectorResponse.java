package com.synapsecore.integration.dto;

import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationImportStatus;
import com.synapsecore.domain.entity.IntegrationSyncMode;
import com.synapsecore.domain.entity.IntegrationTransformationPolicy;
import com.synapsecore.domain.entity.IntegrationValidationPolicy;
import com.synapsecore.integration.IntegrationFailureCode;
import java.time.Instant;
import java.util.List;

public record IntegrationConnectorResponse(
    Long id,
    Long version,
    String tenantCode,
    String sourceSystem,
    IntegrationConnectorType type,
    String displayName,
    boolean enabled,
    IntegrationSyncMode syncMode,
    List<IntegrationSyncMode> supportedSyncModes,
    Integer syncIntervalMinutes,
    String pullEndpointUrl,
    Instant lastPullAttemptAt,
    Instant lastPullSuccessAt,
    String lastPullStatus,
    String lastPullMessage,
    IntegrationValidationPolicy validationPolicy,
    IntegrationTransformationPolicy transformationPolicy,
    Integer mappingVersion,
    boolean allowDefaultWarehouseFallback,
    String defaultWarehouseCode,
    String notes,
    String supportOwnerActorName,
    String supportOwnerDisplayName,
    boolean inboundAccessConfigured,
    String inboundAccessTokenHint,
    IntegrationConnectorHealthStatus healthStatus,
    String healthSummary,
    String supportBoundary,
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
    Long oldestPendingReplayAgeSeconds,
    Instant createdAt,
    Instant updatedAt
) {

    public IntegrationConnectorResponse(
        Long id,
        String tenantCode,
        String sourceSystem,
        IntegrationConnectorType type,
        String displayName,
        boolean enabled,
        IntegrationSyncMode syncMode,
        List<IntegrationSyncMode> supportedSyncModes,
        Integer syncIntervalMinutes,
        String pullEndpointUrl,
        Instant lastPullAttemptAt,
        Instant lastPullSuccessAt,
        String lastPullStatus,
        String lastPullMessage,
        IntegrationValidationPolicy validationPolicy,
        IntegrationTransformationPolicy transformationPolicy,
        Integer mappingVersion,
        boolean allowDefaultWarehouseFallback,
        String defaultWarehouseCode,
        String notes,
        String supportOwnerActorName,
        String supportOwnerDisplayName,
        boolean inboundAccessConfigured,
        String inboundAccessTokenHint,
        IntegrationConnectorHealthStatus healthStatus,
        String healthSummary,
        String supportBoundary,
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
        Long oldestPendingReplayAgeSeconds,
        Instant createdAt,
        Instant updatedAt
    ) {
        this(id, null, tenantCode, sourceSystem, type, displayName, enabled, syncMode, supportedSyncModes,
            syncIntervalMinutes, pullEndpointUrl, lastPullAttemptAt, lastPullSuccessAt, lastPullStatus,
            lastPullMessage, validationPolicy, transformationPolicy, mappingVersion, allowDefaultWarehouseFallback,
            defaultWarehouseCode, notes, supportOwnerActorName, supportOwnerDisplayName, inboundAccessConfigured,
            inboundAccessTokenHint, healthStatus, healthSummary, supportBoundary, lastActivityAt,
            lastSuccessfulActivityAt, lastImportStatus, lastImportAt, recentInboundFailureCount, pendingReplayCount,
            deadLetterCount, lastFailureCode, lastFailureMessage, lastFailureAt, oldestPendingReplayAt,
            oldestPendingReplayAgeSeconds, createdAt, updatedAt);
    }
}
