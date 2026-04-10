package com.synapsecore.scenario.dto;

import com.synapsecore.domain.dto.OrderCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScenarioCompareRequest(
    @Size(max = 40) String primaryLabel,
    @NotNull @Valid OrderCreateRequest primary,
    @Size(max = 40) String alternativeLabel,
    @NotNull @Valid OrderCreateRequest alternative
) {
}
