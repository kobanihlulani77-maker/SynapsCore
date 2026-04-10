package com.synapsecore.scenario.dto;

import com.synapsecore.domain.entity.AlertSeverity;
import com.synapsecore.domain.entity.AlertType;

public record ScenarioAlertProjection(
    AlertType type,
    AlertSeverity severity,
    String title,
    String description,
    String impactSummary,
    String recommendedAction
) {
}
