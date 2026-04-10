package com.synapsecore.access.dto;

import com.synapsecore.domain.entity.IntegrationSyncMode;
import com.synapsecore.domain.entity.IntegrationTransformationPolicy;
import com.synapsecore.domain.entity.IntegrationValidationPolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record TenantWorkspaceConnectorSupportUpdateRequest(
    @Size(max = 80)
    String supportOwnerActorName,

    IntegrationSyncMode syncMode,

    @Min(15) @Max(1440)
    Integer syncIntervalMinutes,

    IntegrationValidationPolicy validationPolicy,

    IntegrationTransformationPolicy transformationPolicy,

    Boolean allowDefaultWarehouseFallback,

    @Size(max = 240)
    String notes
) {
}
