package com.synapsecore.domain.dto;

public enum SystemIncidentType {
    AUDIT_FAILURE,
    INBOUND_REJECTION,
    REPLAY_BACKLOG,
    CONNECTOR_DISABLED,
    CONNECTOR_DEGRADED,
    BACKBONE_DISPATCH_FAILURE,
    CONTROL_NOTICE
}
