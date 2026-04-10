package com.synapsecore.scenario.dto;

import com.synapsecore.domain.entity.ScenarioApprovalStatus;
import com.synapsecore.domain.entity.ScenarioApprovalPolicy;
import com.synapsecore.domain.entity.ScenarioApprovalStage;
import com.synapsecore.domain.entity.ScenarioReviewPriority;
import com.synapsecore.domain.entity.ScenarioRunType;
import java.util.Locale;

public record ScenarioHistoryFilter(
    ScenarioRunType type,
    ScenarioApprovalStatus approvalStatus,
    ScenarioApprovalPolicy approvalPolicy,
    ScenarioApprovalStage approvalStage,
    String warehouseCode,
    String requestedBy,
    String reviewOwner,
    String finalApprovalOwner,
    ScenarioReviewPriority minimumReviewPriority,
    Boolean overdueOnly,
    Boolean slaEscalatedOnly,
    Boolean slaAcknowledged,
    Integer limit
) {

    private static final int DEFAULT_LIMIT = 12;
    private static final int MAX_LIMIT = 40;

    public int resolvedLimit() {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    public String normalizedWarehouseCode() {
        if (warehouseCode == null || warehouseCode.isBlank()) {
            return null;
        }
        return warehouseCode.trim().toUpperCase(Locale.ROOT);
    }

    public String normalizedRequestedBy() {
        if (requestedBy == null || requestedBy.isBlank()) {
            return null;
        }
        return requestedBy.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizedReviewOwner() {
        if (reviewOwner == null || reviewOwner.isBlank()) {
            return null;
        }
        return reviewOwner.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizedFinalApprovalOwner() {
        if (finalApprovalOwner == null || finalApprovalOwner.isBlank()) {
            return null;
        }
        return finalApprovalOwner.trim().toLowerCase(Locale.ROOT);
    }

    public boolean overdueOnlyEnabled() {
        return Boolean.TRUE.equals(overdueOnly);
    }

    public boolean slaEscalatedOnlyEnabled() {
        return Boolean.TRUE.equals(slaEscalatedOnly);
    }

    public Boolean slaAcknowledged() {
        return slaAcknowledged;
    }
}
