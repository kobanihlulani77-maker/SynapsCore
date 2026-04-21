package com.synapsecore.domain.dto;

import com.synapsecore.access.SynapseAccessRole;
import com.synapsecore.domain.entity.AlertSeverity;
import com.synapsecore.domain.entity.RecommendationPriority;
import com.synapsecore.domain.entity.ScenarioReviewPriority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TenantOperationalPolicyRequest(
    @NotNull Double lowStockCriticalRatio,
    @NotNull Double depletionRiskHoursThreshold,
    @NotNull Double urgentDepletionRiskHoursThreshold,
    @NotNull @Min(1) Integer rapidConsumptionUnitsMinimum,
    @NotNull Double rapidConsumptionThresholdRatio,
    @NotNull @Min(1) Integer backlogRiskCount,
    @NotNull @Min(1) Integer backlogCriticalCount,
    @NotNull Double backlogClearHoursThreshold,
    @NotNull @Min(1) Integer delayedShipmentCountThreshold,
    @NotNull @Min(1) Integer overdueDispatchCountThreshold,
    @NotNull Double deliveryDelayToleranceHours,
    @NotNull @Min(1) Integer highRiskScoreThreshold,
    @NotNull @Min(1) Integer criticalRiskScoreThreshold,
    AlertSeverity lowStockSeverity,
    AlertSeverity lowStockCriticalSeverity,
    AlertSeverity depletionRiskSeverity,
    AlertSeverity urgentDepletionRiskSeverity,
    AlertSeverity backlogRiskSeverity,
    AlertSeverity backlogCriticalSeverity,
    AlertSeverity deliveryDelaySeverity,
    AlertSeverity fulfillmentAnomalySeverity,
    RecommendationPriority lowStockRecommendationPriority,
    RecommendationPriority criticalLowStockRecommendationPriority,
    RecommendationPriority depletionRiskRecommendationPriority,
    RecommendationPriority urgentDepletionRiskRecommendationPriority,
    RecommendationPriority backlogRecommendationPriority,
    RecommendationPriority deliveryDelayRecommendationPriority,
    RecommendationPriority fulfillmentAnomalyRecommendationPriority,
    ScenarioReviewPriority escalatedApprovalMinimumPriority,
    @NotNull @Min(1) Integer reviewHoursMedium,
    @NotNull @Min(1) Integer reviewHoursHigh,
    @NotNull @Min(1) Integer reviewHoursCritical,
    @NotNull @Min(1) Integer finalApprovalHoursMedium,
    @NotNull @Min(1) Integer finalApprovalHoursHigh,
    @NotNull @Min(1) Integer finalApprovalHoursCritical,
    @NotNull SynapseAccessRole reviewOwnerRole,
    @NotNull SynapseAccessRole finalApproverRole,
    @NotNull SynapseAccessRole escalationOwnerRole
) {
}
