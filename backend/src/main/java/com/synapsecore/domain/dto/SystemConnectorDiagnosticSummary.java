package com.synapsecore.domain.dto;

import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.integration.IntegrationFailureCode;
import com.synapsecore.integration.dto.IntegrationConnectorHealthStatus;
import java.time.Instant;

public record SystemConnectorDiagnosticSummary(
    String sourceSystem,
    IntegrationConnectorType connectorType,
    String displayName,
    IntegrationConnectorHealthStatus healthStatus,
    String healthSummary,
    IntegrationFailureCode lastFailureCode,
    String lastFailureMessage,
    Instant lastFailureAt,
    long pendingReplayCount,
    long deadLetterCount,
    Long oldestPendingReplayAgeSeconds
) {
}
