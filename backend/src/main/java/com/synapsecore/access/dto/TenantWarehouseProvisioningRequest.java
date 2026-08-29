package com.synapsecore.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantWarehouseProvisioningRequest(
    @NotBlank
    @Size(max = 40)
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "warehouse code must contain only letters, digits, dots, underscores, and hyphens")
    String code,

    @NotBlank
    @Size(max = 120)
    String name,

    @NotBlank
    @Size(max = 120)
    String location
) {
}
