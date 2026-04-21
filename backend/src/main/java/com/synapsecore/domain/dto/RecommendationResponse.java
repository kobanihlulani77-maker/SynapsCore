package com.synapsecore.domain.dto;

import com.synapsecore.domain.entity.RecommendationPriority;
import com.synapsecore.domain.entity.RecommendationType;
import java.time.Instant;

public record RecommendationResponse(
    Long id,
    RecommendationType type,
    String title,
    String description,
    String policyExplanation,
    RecommendationPriority priority,
    Instant createdAt
) {
}
