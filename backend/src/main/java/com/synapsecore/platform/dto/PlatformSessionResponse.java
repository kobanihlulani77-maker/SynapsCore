package com.synapsecore.platform.dto;

import java.time.Instant;

public record PlatformSessionResponse(
    boolean signedIn,
    String username,
    String displayName,
    Instant authenticatedAt,
    Instant expiresAt
) {
}
