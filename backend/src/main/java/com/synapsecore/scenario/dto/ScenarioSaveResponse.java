package com.synapsecore.scenario.dto;

import com.synapsecore.domain.entity.ScenarioApprovalStatus;
import com.synapsecore.domain.entity.ScenarioApprovalPolicy;
import com.synapsecore.domain.entity.ScenarioApprovalStage;
import com.synapsecore.domain.entity.ScenarioReviewPriority;
import com.synapsecore.domain.entity.ScenarioRunType;
import java.time.Instant;

public record ScenarioSaveResponse(
    Long scenarioRunId,
    ScenarioRunType type,
    String title,
    String warehouseCode,
    String requestedBy,
    String reviewOwner,
    String finalApprovalOwner,
    ScenarioReviewPriority reviewPriority,
    Integer riskScore,
    ScenarioApprovalPolicy approvalPolicy,
    ScenarioApprovalStage approvalStage,
    Long revisionOfScenarioRunId,
    Integer revisionNumber,
    boolean executable,
    ScenarioApprovalStatus approvalStatus,
    Instant approvalDueAt,
    String slaEscalatedTo,
    Instant slaEscalatedAt,
    boolean slaEscalated,
    boolean overdue,
    Instant savedAt
) {
}
