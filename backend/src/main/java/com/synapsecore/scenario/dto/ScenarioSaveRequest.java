package com.synapsecore.scenario.dto;

import com.synapsecore.domain.dto.OrderCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScenarioSaveRequest(
    @NotBlank @Size(max = 120) String title,
    @Size(max = 80) String requestedBy,
    @Size(max = 80) String reviewOwner,
    Long revisionOfScenarioRunId,
    @NotNull @Valid OrderCreateRequest request
) {
}
