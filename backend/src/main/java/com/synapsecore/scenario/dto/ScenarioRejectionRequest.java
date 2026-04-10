package com.synapsecore.scenario.dto;

import com.synapsecore.scenario.ScenarioActorRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScenarioRejectionRequest(
    @NotNull ScenarioActorRole actorRole,
    @NotBlank @Size(max = 80) String reviewerName,
    @NotBlank @Size(max = 240) String reason
) {
}
