package com.synapsecore.scenario.dto;

import com.synapsecore.domain.dto.OrderResponse;
import java.time.Instant;

public record ScenarioExecutionResponse(
    Long scenarioRunId,
    String scenarioTitle,
    OrderResponse order,
    Instant executedAt
) {
}
