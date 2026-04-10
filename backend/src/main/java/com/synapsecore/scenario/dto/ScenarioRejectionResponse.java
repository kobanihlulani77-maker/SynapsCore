package com.synapsecore.scenario.dto;

import com.synapsecore.domain.entity.ScenarioApprovalStatus;
import java.time.Instant;

public record ScenarioRejectionResponse(
    Long scenarioRunId,
    String title,
    ScenarioApprovalStatus approvalStatus,
    String rejectedBy,
    Instant rejectedAt,
    String rejectionReason
) {
}
