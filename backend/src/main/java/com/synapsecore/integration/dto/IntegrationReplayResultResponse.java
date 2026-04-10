package com.synapsecore.integration.dto;

import com.synapsecore.domain.dto.OrderResponse;
import java.time.Instant;

public record IntegrationReplayResultResponse(
    IntegrationReplayRecordResponse replay,
    OrderResponse order,
    Instant replayedAt
) {
}
