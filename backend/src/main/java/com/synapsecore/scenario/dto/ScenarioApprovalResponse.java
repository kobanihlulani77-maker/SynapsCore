package com.synapsecore.scenario.dto;

import com.synapsecore.domain.entity.ScenarioApprovalStatus;
import com.synapsecore.domain.entity.ScenarioApprovalPolicy;
import com.synapsecore.domain.entity.ScenarioApprovalStage;
import java.time.Instant;

public record ScenarioApprovalResponse(
    Long scenarioRunId,
    String title,
    ScenarioApprovalStatus approvalStatus,
    ScenarioApprovalPolicy approvalPolicy,
    ScenarioApprovalStage approvalStage,
    String finalApprovalOwner,
    String reviewApprovedBy,
    Instant reviewApprovedAt,
    String approvedBy,
    String approvalNote,
    Instant approvedAt,
    Instant approvalDueAt,
    String slaEscalatedTo,
    Instant slaEscalatedAt,
    boolean slaEscalated,
    boolean overdue,
    boolean executionReady
) {
}
