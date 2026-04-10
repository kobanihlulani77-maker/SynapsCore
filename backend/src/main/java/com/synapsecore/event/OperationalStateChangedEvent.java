package com.synapsecore.event;

import java.time.Instant;

public record OperationalStateChangedEvent(
    OperationalUpdateType updateType,
    String tenantCode,
    String source,
    String requestId,
    Instant occurredAt
) {
}
