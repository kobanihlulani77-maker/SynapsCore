package com.synapsecore.scenario.dto;

import com.synapsecore.scenario.ScenarioActorRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScenarioApprovalRequest(
    @NotNull ScenarioActorRole actorRole,
    @NotBlank @Size(max = 80) String approverName,
    @Size(max = 240) String approvalNote
) {
}
