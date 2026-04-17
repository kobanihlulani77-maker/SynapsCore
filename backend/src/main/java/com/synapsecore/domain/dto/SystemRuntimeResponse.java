package com.synapsecore.domain.dto;

import java.time.Instant;
import java.util.List;

public record SystemRuntimeResponse(
    String applicationName,
    SystemBuildInfo build,
    List<String> activeProfiles,
    String overallStatus,
    String livenessState,
    String readinessState,
    boolean headerFallbackEnabled,
    boolean secureSessionCookies,
    List<String> allowedOrigins,
    String publicAppUrl,
    String publicApiUrl,
    long simulationIntervalMs,
    SystemTelemetrySummary telemetry,
    SystemBackboneSummary backbone,
    SystemMetricsSummary metrics,
    SystemDiagnosticsSummary diagnostics,
    List<SystemConnectorDiagnosticSummary> connectorDiagnostics,
    Instant observedAt
) {
}
