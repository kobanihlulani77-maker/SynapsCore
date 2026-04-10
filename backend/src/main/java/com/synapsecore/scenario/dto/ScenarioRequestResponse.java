package com.synapsecore.scenario.dto;

import com.synapsecore.domain.dto.OrderCreateRequest;
import java.time.Instant;

public record ScenarioRequestResponse(
    Long scenarioRunId,
    String scenarioTitle,
    OrderCreateRequest request,
    Instant loadedAt
) {
}
