package com.synapsecore.access.dto;

import com.synapsecore.domain.dto.WarehouseResponse;
import com.synapsecore.domain.dto.SystemIncidentResponse;
import com.synapsecore.integration.dto.IntegrationConnectorResponse;
import com.synapsecore.access.SynapseAccessRole;
import java.time.Instant;
import java.util.List;

public record TenantWorkspaceResponse(
    Long tenantId,
    String tenantCode,
    String tenantName,
    String description,
    boolean active,
    TenantWorkspaceSecuritySettings securitySettings,
    TenantWorkspaceSupportSummary supportSummary,
    TenantWorkspaceSupportDiagnostics supportDiagnostics,
    List<SystemIncidentResponse> supportIncidents,
    List<TenantWorkspaceSupportActivity> recentSupportActivity,
    List<WarehouseResponse> warehouses,
    List<IntegrationConnectorResponse> connectors,
    List<SynapseAccessRole> requiredRoles,
    TenantWorkspaceReadiness readiness,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {
}
