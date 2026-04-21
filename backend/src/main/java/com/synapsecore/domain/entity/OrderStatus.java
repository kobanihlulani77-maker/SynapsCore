package com.synapsecore.domain.entity;

public enum OrderStatus {
    CREATED,
    RECEIVED,
    PROCESSING,
    PARTIALLY_FULFILLED,
    FULFILLED,
    DELIVERED,
    CANCELLED,
    RETURNED,
    FAILED,
    BLOCKED
}
