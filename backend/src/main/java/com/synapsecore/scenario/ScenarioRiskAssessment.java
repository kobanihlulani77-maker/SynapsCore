package com.synapsecore.scenario;

import com.synapsecore.domain.entity.ScenarioReviewPriority;

public record ScenarioRiskAssessment(
    int score,
    ScenarioReviewPriority reviewPriority
) {
}
