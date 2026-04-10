package com.synapsecore.access.dto;

import java.time.Instant;

public record TenantWorkspaceSupportDiagnostics(
    long activeUsersRequiringPasswordChange,
    long activeUsersPastPasswordRotation,
    long activeUsersBlockedByInactiveOperator,
    long connectorsWithoutSupportOwner,
    long highSeverityIncidentCount,
    Instant latestSupportAuditAt
) {
}
