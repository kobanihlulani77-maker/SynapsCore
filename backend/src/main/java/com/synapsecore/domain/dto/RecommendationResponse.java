package com.synapsecore.domain.dto;

import com.synapsecore.domain.entity.RecommendationPriority;
import com.synapsecore.domain.entity.RecommendationType;
import com.synapsecore.domain.entity.RecommendationStatus;
import java.time.Instant;

public record RecommendationResponse(
    Long id,
    RecommendationType type,
    RecommendationPriority priority,
    RecommendationStatus status,
    String warehouseCode,
    String sourceWarehouseCode,
    String destinationWarehouseCode,
    String productSku,
    String sourceType,
    String sourceRef,
    String title,
    String description,
    String policyExplanation,
    Instant createdAt,
    Instant updatedAt,
    Long suggestedQuantity
) {

    public RecommendationResponse(Long id,
                                  RecommendationType type,
                                  String title,
                                  String description,
                                  String policyExplanation,
                                  RecommendationPriority priority,
                                  Instant createdAt) {
        this(id, type, priority, RecommendationStatus.CURRENT, null, null, null, null, null, null,
            title, description, policyExplanation, createdAt, createdAt, null);
    }
}
