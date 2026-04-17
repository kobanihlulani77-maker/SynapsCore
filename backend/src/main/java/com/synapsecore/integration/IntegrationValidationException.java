package com.synapsecore.integration;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class IntegrationValidationException extends ResponseStatusException {

    private final IntegrationFailureCode failureCode;

    public IntegrationValidationException(HttpStatus status, IntegrationFailureCode failureCode, String reason) {
        super(status, reason);
        this.failureCode = failureCode;
    }

    public IntegrationFailureCode getFailureCode() {
        return failureCode;
    }
}
