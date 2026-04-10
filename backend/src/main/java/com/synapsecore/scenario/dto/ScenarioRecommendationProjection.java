package com.synapsecore.scenario.dto;

import com.synapsecore.domain.entity.RecommendationPriority;
import com.synapsecore.domain.entity.RecommendationType;

public record ScenarioRecommendationProjection(
    RecommendationType type,
    RecommendationPriority priority,
    String title,
    String description
) {
}
