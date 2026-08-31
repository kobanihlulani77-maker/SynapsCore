package com.synapsecore.platform;

import java.time.Instant;

public record PlatformActivityChangedNotification(
    String type,
    Instant occurredAt,
    String scope
) {
    public static PlatformActivityChangedNotification now() {
        return new PlatformActivityChangedNotification(
            "PLATFORM_ACTIVITY_CHANGED",
            Instant.now(),
            "PLATFORM"
        );
    }
}
