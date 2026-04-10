package com.synapsecore.integration.dto;

import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.entity.IntegrationImportStatus;
import java.time.Instant;

public record IntegrationImportRunResponse(
    Long id,
    String sourceSystem,
    IntegrationConnectorType connectorType,
    String fileName,
    int recordsReceived,
    int ordersImported,
    int ordersFailed,
    IntegrationImportStatus status,
    String summary,
    Instant createdAt
) {
}
