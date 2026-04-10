package com.synapsecore.domain.dto;

import java.util.List;

public record AlertFeedResponse(
    List<AlertResponse> activeAlerts,
    List<AlertResponse> recentAlerts
) {
}
