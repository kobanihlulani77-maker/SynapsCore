package com.synapsecore.platform;

import com.synapsecore.domain.entity.IntegrationInboundStatus;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.domain.entity.AlertStatus;
import com.synapsecore.domain.entity.AuditLog;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.entity.BusinessEvent;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.repository.AccessOperatorRepository;
import com.synapsecore.domain.repository.AccessUserRepository;
import com.synapsecore.domain.repository.AlertRepository;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.IntegrationInboundRecordRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.TenantRepository;
import com.synapsecore.domain.repository.TenantCount;
import com.synapsecore.domain.service.SystemRuntimeService;
import com.synapsecore.observability.ThreadCpuTiming;
import com.synapsecore.platform.dto.PlatformActivityResponse;
import com.synapsecore.platform.dto.PlatformOverviewResponse;
import com.synapsecore.platform.dto.PlatformRuntimeResponse;
import com.synapsecore.platform.dto.PlatformTenantSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformControlPlaneService {

    private static final Logger log = LoggerFactory.getLogger(PlatformControlPlaneService.class);
    private static final long SLOW_OVERVIEW_NANOS = 2_000_000_000L;

    private final TenantRepository tenantRepository;
    private final AccessUserRepository accessUserRepository;
    private final AccessOperatorRepository accessOperatorRepository;
    private final IntegrationConnectorRepository integrationConnectorRepository;
    private final IntegrationInboundRecordRepository integrationInboundRecordRepository;
    private final IntegrationReplayRecordRepository integrationReplayRecordRepository;
    private final AlertRepository alertRepository;
    private final BusinessEventRepository businessEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final SystemRuntimeService systemRuntimeService;

    public PlatformOverviewResponse getOverview() {
        long startedAt = System.nanoTime();
        Map<String, Long> sectionsMs = new LinkedHashMap<>();
        Map<String, Long> sectionsCpuMs = new LinkedHashMap<>();
        try {
            PlatformRuntimeResponse runtime = timedOverviewSection(sectionsMs, sectionsCpuMs, "runtime", this::getRuntime);
            List<PlatformTenantSummary> tenants = timedOverviewSection(sectionsMs, sectionsCpuMs, "tenants", this::getTenants);
            List<PlatformActivityResponse> activity = timedOverviewSection(sectionsMs, sectionsCpuMs, "activity", this::getActivity);
            return new PlatformOverviewResponse(runtime, tenants, activity, Instant.now());
        } finally {
            long elapsed = System.nanoTime() - startedAt;
            if (elapsed >= SLOW_OVERVIEW_NANOS) {
                log.warn("Slow platform overview composition totalMs={} sectionsMs={} sectionsCpuMs={}",
                    elapsed / 1_000_000, sectionsMs, sectionsCpuMs);
            }
        }
    }

    private <T> T timedOverviewSection(Map<String, Long> sectionsMs, Map<String, Long> sectionsCpuMs,
                                       String section, Supplier<T> read) {
        long startedAt = System.nanoTime();
        long cpuStartedAt = ThreadCpuTiming.currentNanos();
        try {
            return read.get();
        } finally {
            sectionsMs.put(section, (System.nanoTime() - startedAt) / 1_000_000);
            sectionsCpuMs.put(section, ThreadCpuTiming.elapsedMillis(cpuStartedAt));
        }
    }

    @Transactional(readOnly = true)
    public List<PlatformTenantSummary> getTenants() {
        Map<String, Long> activeUsers = tenantCounts(accessUserRepository.countActiveByTenant());
        Map<String, Long> activeOperators = tenantCounts(accessOperatorRepository.countActiveByTenant());
        Map<String, Long> connectors = tenantCounts(integrationConnectorRepository.countByTenantGrouped());
        Map<String, Long> disabled = tenantCounts(integrationConnectorRepository.countDisabledByTenant());
        Map<String, Long> failedInbounds = tenantCounts(integrationInboundRecordRepository.countByTenantAndStatusInGrouped(
            List.of(IntegrationInboundStatus.REJECTED, IntegrationInboundStatus.REPLAY_QUEUED)));
        Map<String, Long> replayAttentionCounts = tenantCounts(integrationReplayRecordRepository.countByTenantAndStatusInGrouped(
            List.of(IntegrationReplayStatus.PENDING, IntegrationReplayStatus.REPLAY_FAILED,
                IntegrationReplayStatus.DEAD_LETTERED)));
        Map<String, Long> activeAlerts = tenantCounts(alertRepository.countByTenantAndStatusGrouped(AlertStatus.ACTIVE));
        return tenantRepository.findAllByOrderByNameAsc().stream()
            .map(tenant -> {
                String tenantCode = tenant.getCode();
                String key = tenantCode.toLowerCase(Locale.ROOT);
                long disabledConnectors = disabled.getOrDefault(key, 0L);
                long failedInbound = failedInbounds.getOrDefault(key, 0L);
                long replayAttention = replayAttentionCounts.getOrDefault(key, 0L);
                long alertCount = activeAlerts.getOrDefault(key, 0L);
                String supportState = !tenant.isActive()
                    ? "INACTIVE"
                    : disabledConnectors + failedInbound + replayAttention + alertCount > 0 ? "ATTENTION" : "HEALTHY";
                return new PlatformTenantSummary(
                    tenant.getId(),
                    tenantCode,
                    tenant.getName(),
                    tenant.isActive(),
                    activeUsers.getOrDefault(key, 0L),
                    activeOperators.getOrDefault(key, 0L),
                    connectors.getOrDefault(key, 0L),
                    disabledConnectors,
                    failedInbound,
                    replayAttention,
                    alertCount,
                    supportState,
                    tenant.getUpdatedAt()
                );
            })
            .toList();
    }

    private Map<String, Long> tenantCounts(List<TenantCount> counts) {
        return counts.stream().collect(Collectors.toMap(TenantCount::tenantCode, TenantCount::count));
    }

    @Transactional(readOnly = true)
    public List<PlatformActivityResponse> getActivity() {
        long startedAt = System.nanoTime();
        Map<String, Long> stepsMs = new LinkedHashMap<>();
        Map<String, Long> stepsCpuMs = new LinkedHashMap<>();
        try {
            List<BusinessEvent> events = timedOverviewSection(stepsMs, stepsCpuMs, "events",
                businessEventRepository::findTop20ByOrderByCreatedAtDesc);
            List<AuditLog> audits = timedOverviewSection(stepsMs, stepsCpuMs, "audits",
                auditLogRepository::findTop20ByOrderByCreatedAtDesc);
            return timedOverviewSection(stepsMs, stepsCpuMs, "merge", () -> {
                List<PlatformActivityResponse> activity = new ArrayList<>();
                events.forEach(event -> activity.add(toBusinessEventActivity(event)));
                audits.forEach(audit -> activity.add(toAuditActivity(audit)));
                return activity.stream()
                    .sorted(Comparator.comparing(PlatformActivityResponse::observedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(20)
                    .toList();
            });
        } finally {
            long elapsed = System.nanoTime() - startedAt;
            if (elapsed >= SLOW_OVERVIEW_NANOS) {
                log.warn("Slow platform activity read totalMs={} stepsMs={} stepsCpuMs={}",
                    elapsed / 1_000_000, stepsMs, stepsCpuMs);
            }
        }
    }

    public PlatformRuntimeResponse getRuntime() {
        return systemRuntimeService.getPlatformRuntimeStatus();
    }

    private String normalizeTenantCode(String tenantCode) {
        return tenantCode == null || tenantCode.isBlank() ? "PLATFORM" : tenantCode.trim();
    }

    private PlatformActivityResponse toBusinessEventActivity(BusinessEvent event) {
        String tenantCode = normalizeTenantCode(event.getTenantCode());
        boolean platformScope = "PLATFORM".equalsIgnoreCase(tenantCode);
        return new PlatformActivityResponse(
            tenantCode,
            businessEventCategory(event.getEventType()),
            event.getEventType().name(),
            "RECORDED",
            event.getCreatedAt(),
            platformScope ? "Platform" : "Tenant",
            "Success",
            platformScope ? "None" : "Tenant-specific",
            "Informational",
            "The " + formatSignal(event.getEventType().name()) + " event was recorded in the operational timeline.",
            "No action"
        );
    }

    private PlatformActivityResponse toAuditActivity(AuditLog log) {
        String tenantCode = normalizeTenantCode(log.getTenantCode());
        boolean platformRequest = isPlatformRequest(log);
        String action = log.getAction() == null ? "" : log.getAction().trim().toUpperCase(Locale.ROOT);
        String details = log.getDetails() == null ? "" : log.getDetails().trim();
        SignalInterpretation interpretation = interpretAudit(log, platformRequest);
        return new PlatformActivityResponse(
            tenantCode,
            auditCategory(log, action, details),
            log.getAction(),
            log.getStatus().name(),
            log.getCreatedAt(),
            interpretation.scope(),
            interpretation.classification(),
            interpretation.impact(),
            interpretation.severity(),
            interpretation.interpretation(),
            interpretation.nextAction()
        );
    }

    private SignalInterpretation interpretAudit(AuditLog log, boolean platformRequest) {
        String action = log.getAction() == null ? "" : log.getAction().trim().toUpperCase(Locale.ROOT);
        String details = log.getDetails() == null ? "" : log.getDetails().trim();
        boolean missingTenant = "tenant-context-missing".equalsIgnoreCase(log.getTenantCode());
        boolean platformScope = platformRequest && ("PLATFORM".equalsIgnoreCase(normalizeTenantCode(log.getTenantCode())) || missingTenant);
        String scope = platformScope ? "Platform" : missingTenant || log.getTenantCode() == null || log.getTenantCode().isBlank()
            ? "Unknown / unattributed" : "Tenant";
        boolean expectedDenial = isExpectedDenial(log, details);

        if ("PLATFORM_AUTH_LOGIN".equals(action) && log.getStatus() == AuditStatus.FAILURE) {
            return new SignalInterpretation(
                "Platform",
                "Expected denial",
                "None",
                "Low",
                "Platform authentication was rejected; the security control responded as expected and no tenant operational impact is indicated.",
                "Monitor"
            );
        }
        if (missingTenant && !platformRequest) {
            return new SignalInterpretation(
                "Unknown / unattributed",
                "Warning",
                "Unknown",
                "Medium",
                "Tenant context was missing for a request that may require tenant scoping.",
                "Investigate"
            );
        }
        if (missingTenant && platformRequest && log.getStatus() == AuditStatus.SUCCESS) {
            return new SignalInterpretation(
                "Platform",
                "Success",
                "None",
                "Informational",
                "No tenant context was expected for this platform request; the platform operation completed and was recorded.",
                "No action"
            );
        }
        if (expectedDenial) {
            return new SignalInterpretation(
                scope,
                "Expected denial",
                "None",
                "Informational",
                platformScope
                    ? "The platform request was denied without requiring tenant context; this is security evidence, not tenant health evidence."
                    : "The request was denied by an authorization or validation control; this is not evidence of an operational failure.",
                "Monitor"
            );
        }
        if (log.getStatus() == AuditStatus.SUCCESS) {
            return new SignalInterpretation(
                scope,
                "Success",
                "None",
                "Informational",
                "The " + formatSignal(action) + " action completed and was recorded in the audit timeline.",
                "No action"
            );
        }
        return new SignalInterpretation(
            scope,
            "Operational failure",
            platformScope ? "Platform-wide" : "Tenant-specific",
            platformScope ? "High" : "Medium",
            "The " + formatSignal(action) + " action was recorded as a failure. Inspect the related runtime or tenant evidence before continuing.",
            platformScope ? "Inspect Platform Runtime" : "Investigate"
        );
    }

    private String auditCategory(AuditLog log, String action, String details) {
        String source = log.getSource() == null ? "" : log.getSource().toLowerCase(Locale.ROOT);
        if (action.contains("AUTH") || action.contains("LOGIN") || action.contains("SESSION")) return "Authentication";
        if ("REQUEST_REJECTED".equals(action) && isExpectedDenial(log, details)) return "Authorization";
        if (action.contains("INTEGRATION") || source.contains("integration")) return "Integration";
        if (action.contains("DEPLOY") || action.contains("RELEASE")) return "Deployment";
        if (action.contains("REDIS") || action.contains("DATABASE") || action.contains("POSTGRES")) return "Infrastructure";
        if (source.contains("runtime") || source.contains("websocket") || source.contains("/ws")) return "Runtime";
        if ("REQUEST_REJECTED".equals(action)) return "Request";
        return "Request";
    }

    private String businessEventCategory(BusinessEventType eventType) {
        return eventType.name().startsWith("INTEGRATION_") ? "Integration" : "Runtime";
    }

    private boolean isPlatformRequest(AuditLog log) {
        String source = log.getSource() == null ? "" : log.getSource().toLowerCase(Locale.ROOT);
        String action = log.getAction() == null ? "" : log.getAction().toUpperCase(Locale.ROOT);
        return source.contains("/api/platform/") || source.contains("platform-session") || action.startsWith("PLATFORM_");
    }

    private boolean isExpectedDenial(AuditLog log, String details) {
        if (log.getStatus() != AuditStatus.FAILURE) return false;
        String action = log.getAction() == null ? "" : log.getAction().trim();
        return "PLATFORM_AUTH_LOGIN".equalsIgnoreCase(action)
            || ("REQUEST_REJECTED".equalsIgnoreCase(action) && details.matches("^(401|403)\\b.*"));
    }

    private String formatSignal(String value) {
        if (value == null || value.isBlank()) return "unknown";
        return value.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private record SignalInterpretation(
        String scope,
        String classification,
        String impact,
        String severity,
        String interpretation,
        String nextAction
    ) {
    }
}
