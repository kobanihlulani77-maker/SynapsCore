package com.synapsecore.api.controller;

import java.time.Instant;

public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String requestId
) {
}
