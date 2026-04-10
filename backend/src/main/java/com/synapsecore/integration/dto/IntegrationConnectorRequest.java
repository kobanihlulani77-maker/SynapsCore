package com.synapsecore.integration.dto;

import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationSyncMode;
import com.synapsecore.domain.entity.IntegrationTransformationPolicy;
import com.synapsecore.domain.entity.IntegrationValidationPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IntegrationConnectorRequest(
    @NotBlank
    @Pattern(
        regexp = "^[A-Za-z0-9_-]+$",
        message = "sourceSystem may only contain letters, numbers, hyphens, and underscores"
    )
    String sourceSystem,
    @NotNull IntegrationConnectorType type,
    @NotBlank @Size(max = 120) String displayName,
    boolean enabled,
    IntegrationSyncMode syncMode,
    @Min(15) @Max(1440) Integer syncIntervalMinutes,
    IntegrationValidationPolicy validationPolicy,
    IntegrationTransformationPolicy transformationPolicy,
    Boolean allowDefaultWarehouseFallback,
    @Size(max = 40) String defaultWarehouseCode,
    @Size(max = 240) String notes
) {
}
