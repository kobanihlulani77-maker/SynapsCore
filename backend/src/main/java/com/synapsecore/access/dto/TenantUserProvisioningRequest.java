package com.synapsecore.access.dto;

import com.synapsecore.access.SynapseAccessRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TenantUserProvisioningRequest(
    @NotBlank
    @Size(max = 80)
    String username,

    @NotBlank
    @Size(max = 120)
    String fullName,

    @NotBlank
    @Size(max = 80)
    String operatorActorName,

    @Size(max = 80)
    String operatorDisplayName,

    @Size(max = 160)
    String operatorDescription,

    List<SynapseAccessRole> roles,

    List<@Size(max = 40) String> warehouseScopes,

    @Size(min = 8, max = 120)
    String initialPassword
) {
}
