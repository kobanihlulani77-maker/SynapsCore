package com.synapsecore.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantWorkspaceUpdateRequest(
    @NotBlank
    @Size(max = 120)
    String tenantName,

    @Size(max = 240)
    String description
) {
}
