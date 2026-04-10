package com.synapsecore.scenario.dto;

import com.synapsecore.scenario.ScenarioActorRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScenarioEscalationAcknowledgementRequest(
    @NotNull ScenarioActorRole actorRole,
    @NotBlank @Size(max = 80) String acknowledgedBy,
    @NotBlank @Size(max = 240) String note
) {
}
