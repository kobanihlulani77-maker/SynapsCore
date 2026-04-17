package com.synapsecore.integration;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class IntegrationFailureCodes {

    private IntegrationFailureCodes() {
    }

    public static IntegrationValidationException badRequest(IntegrationFailureCode code, String message) {
        return status(HttpStatus.BAD_REQUEST, code, message);
    }

    public static IntegrationValidationException status(HttpStatus status,
                                                        IntegrationFailureCode code,
                                                        String message) {
        return new IntegrationValidationException(status, code, message);
    }

    public static IntegrationFailureExceptionDetails extract(ResponseStatusException exception) {
        if (exception instanceof IntegrationValidationException validationException) {
            return new IntegrationFailureExceptionDetails(validationException.getFailureCode(), validationException.getReason());
        }
        return new IntegrationFailureExceptionDetails(mapKnownReason(exception.getReason()), exception.getReason());
    }

    private static IntegrationFailureCode mapKnownReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return IntegrationFailureCode.UNKNOWN;
        }
        if (reason.startsWith("Warehouse not found:")) {
            return IntegrationFailureCode.WAREHOUSE_NOT_FOUND;
        }
        if (reason.startsWith("Product not found:")) {
            return IntegrationFailureCode.PRODUCT_NOT_FOUND;
        }
        if (reason.startsWith("No inventory found for SKU")) {
            return IntegrationFailureCode.INVENTORY_NOT_FOUND;
        }
        if (reason.startsWith("Insufficient inventory for SKU")) {
            return IntegrationFailureCode.INSUFFICIENT_INVENTORY;
        }
        if (reason.startsWith("Order already exists for externalOrderId")) {
            return IntegrationFailureCode.DUPLICATE_EXTERNAL_ORDER_ID;
        }
        if (reason.startsWith("Integration connector not configured for sourceSystem")) {
            return IntegrationFailureCode.CONNECTOR_NOT_CONFIGURED;
        }
        if (reason.startsWith("Connector token is invalid for sourceSystem")
            || reason.startsWith("A non-empty connector token is required")) {
            return IntegrationFailureCode.INVALID_CONNECTOR_TOKEN;
        }
        if (reason.contains(" is disabled and cannot ")) {
            return IntegrationFailureCode.CONNECTOR_DISABLED;
        }
        if (reason.startsWith("sourceSystem is required")) {
            return IntegrationFailureCode.INVALID_SOURCE_SYSTEM;
        }
        return IntegrationFailureCode.UNKNOWN;
    }

    public record IntegrationFailureExceptionDetails(
        IntegrationFailureCode failureCode,
        String failureMessage
    ) {
    }
}
