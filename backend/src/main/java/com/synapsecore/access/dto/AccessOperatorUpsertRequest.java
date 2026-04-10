package com.synapsecore.access.dto;

import com.synapsecore.access.SynapseAccessRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AccessOperatorUpsertRequest(
    @NotBlank
    @Size(max = 80)
    String actorName,

    @NotBlank
    @Size(max = 80)
    String displayName,

    @Size(max = 160)
    String description,

    boolean active,

    List<SynapseAccessRole> roles,

    List<@Size(max = 40) String> warehouseScopes
) {
}
