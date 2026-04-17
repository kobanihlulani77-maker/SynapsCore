package com.synapsecore.integration.dto;

import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.integration.IntegrationFailureCode;
import java.time.Instant;

public record IntegrationReplayRecordResponse(
    Long id,
    String sourceSystem,
    IntegrationConnectorType connectorType,
    String externalOrderId,
    String warehouseCode,
    IntegrationFailureCode failureCode,
    String failureMessage,
    IntegrationReplayStatus status,
    int replayAttemptCount,
    String lastReplayMessage,
    Instant lastAttemptedAt,
    Instant nextEligibleAt,
    Instant resolvedAt,
    Instant deadLetteredAt,
    String replayedOrderExternalId,
    Instant createdAt,
    Instant updatedAt
) {
}
