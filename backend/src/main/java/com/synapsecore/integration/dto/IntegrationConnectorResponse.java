package com.synapsecore.integration.dto;

import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationSyncMode;
import com.synapsecore.domain.entity.IntegrationTransformationPolicy;
import com.synapsecore.domain.entity.IntegrationValidationPolicy;
import java.time.Instant;

public record IntegrationConnectorResponse(
    Long id,
    String tenantCode,
    String sourceSystem,
    IntegrationConnectorType type,
    String displayName,
    boolean enabled,
    IntegrationSyncMode syncMode,
    Integer syncIntervalMinutes,
    IntegrationValidationPolicy validationPolicy,
    IntegrationTransformationPolicy transformationPolicy,
    boolean allowDefaultWarehouseFallback,
    String defaultWarehouseCode,
    String notes,
    String supportOwnerActorName,
    String supportOwnerDisplayName,
    Instant createdAt,
    Instant updatedAt
) {
}
