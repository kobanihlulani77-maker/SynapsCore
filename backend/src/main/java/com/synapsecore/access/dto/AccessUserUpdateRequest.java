package com.synapsecore.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccessUserUpdateRequest(
    @NotBlank
    @Size(max = 120)
    String fullName,

    boolean active,

    @NotBlank
    @Size(max = 80)
    String operatorActorName
) {
}
