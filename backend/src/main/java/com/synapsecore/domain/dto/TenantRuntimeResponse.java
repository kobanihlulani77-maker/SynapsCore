package com.synapsecore.domain.dto;

import java.time.Instant;
import java.util.List;

public record TenantRuntimeResponse(
    String applicationName,
    SystemBuildInfo build,
    String overallStatus,
    String livenessState,
    String readinessState,
    boolean secureSessionCookies,
    SystemTelemetrySummary telemetry,
    TenantBackboneSummary backbone,
    SystemMetricsSummary metrics,
    SystemDiagnosticsSummary diagnostics,
    List<SystemConnectorDiagnosticSummary> connectorDiagnostics,
    Instant observedAt
) {
}
