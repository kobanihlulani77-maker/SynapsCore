package com.synapsecore.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantWorkspaceWarehouseCreateRequest(
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Warehouse code may only contain letters, numbers, hyphens, and underscores")
    @Size(max = 40)
    String code,

    @NotBlank @Size(max = 120) String name,

    @NotBlank @Size(max = 120) String location
) {
}
