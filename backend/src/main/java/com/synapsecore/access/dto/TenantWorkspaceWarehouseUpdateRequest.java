package com.synapsecore.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantWorkspaceWarehouseUpdateRequest(
    @NotBlank
    @Size(max = 120)
    String name,

    @NotBlank
    @Size(max = 120)
    String location
) {
}
