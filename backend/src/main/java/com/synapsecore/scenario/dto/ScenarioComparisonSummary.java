package com.synapsecore.scenario.dto;

public record ScenarioComparisonSummary(
    String recommendedOption,
    int primaryRiskScore,
    int alternativeRiskScore,
    String rationale
) {
}
