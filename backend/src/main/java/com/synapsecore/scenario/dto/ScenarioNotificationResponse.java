package com.synapsecore.scenario.dto;

import com.synapsecore.domain.entity.ScenarioApprovalStage;
import com.synapsecore.domain.entity.ScenarioReviewPriority;
import java.time.Instant;

public record ScenarioNotificationResponse(
    Long scenarioRunId,
    ScenarioNotificationType type,
    String title,
    String message,
    String warehouseCode,
    ScenarioReviewPriority reviewPriority,
    ScenarioApprovalStage approvalStage,
    String actor,
    String note,
    boolean actionRequired,
    Instant dueAt,
    Instant createdAt
) {
}
