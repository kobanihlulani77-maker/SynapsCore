package com.synapsecore.integration.dto;

import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import java.time.Instant;

public record IntegrationReplayRecordResponse(
    Long id,
    String sourceSystem,
    IntegrationConnectorType connectorType,
    String externalOrderId,
    String warehouseCode,
    String failureMessage,
    IntegrationReplayStatus status,
    int replayAttemptCount,
    String lastReplayMessage,
    Instant lastAttemptedAt,
    Instant resolvedAt,
    String replayedOrderExternalId,
    Instant createdAt,
    Instant updatedAt
) {
}
