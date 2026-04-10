package com.synapsecore.scenario.dto;

import java.time.Instant;

public record ScenarioComparisonResponse(
    String primaryLabel,
    ScenarioOrderImpactResponse primary,
    String alternativeLabel,
    ScenarioOrderImpactResponse alternative,
    ScenarioComparisonSummary summary,
    Instant comparedAt
) {
}
