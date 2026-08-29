package com.synapsecore.domain.service;

import com.synapsecore.config.SynapseAccessProperties;
import com.synapsecore.config.SynapseCorsProperties;
import com.synapsecore.domain.dto.SystemBackboneSummary;
import com.synapsecore.domain.dto.SystemBuildInfo;
import com.synapsecore.domain.dto.SystemConnectorDiagnosticSummary;
import com.synapsecore.domain.dto.SystemDiagnosticsSummary;
import com.synapsecore.domain.dto.SystemMetricsSummary;
import com.synapsecore.domain.dto.SystemTelemetrySummary;
import com.synapsecore.domain.dto.SystemRuntimeResponse;
import com.synapsecore.domain.dto.TenantBackboneSummary;
import com.synapsecore.domain.dto.TenantRuntimeResponse;
import com.synapsecore.platform.dto.PlatformRuntimeResponse;
import com.synapsecore.domain.entity.AuditStatus;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.entity.IntegrationImportStatus;
import com.synapsecore.domain.entity.IntegrationInboundStatus;
import com.synapsecore.domain.entity.IntegrationReplayStatus;
import com.synapsecore.alert.AlertScopeService;
import com.synapsecore.domain.entity.FulfillmentStatus;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.repository.AuditLogRepository;
import com.synapsecore.domain.repository.BusinessEventRepository;
import com.synapsecore.domain.repository.FulfillmentTaskRepository;
import com.synapsecore.domain.repository.IntegrationConnectorRepository;
import com.synapsecore.domain.repository.IntegrationInboundRecordRepository;
import com.synapsecore.domain.repository.IntegrationImportRunRepository;
import com.synapsecore.domain.repository.IntegrationReplayRecordRepository;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.event.OperationalDispatchQueueService;
import com.synapsecore.integration.IntegrationConnectorService;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.observability.OperationalAlertHookService;
import com.synapsecore.realtime.RealtimeBrokerMode;
import com.synapsecore.realtime.RealtimeService;
import com.synapsecore.tenant.TenantContextService;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemRuntimeService {

    private final Environment environment;
    private final SynapseAccessProperties accessProperties;
    private final SynapseCorsProperties corsProperties;
    private final ApplicationAvailability applicationAvailability;
    private final AlertScopeService alertScopeService;
    private final AuditLogRepository auditLogRepository;
    private final BusinessEventRepository businessEventRepository;
    private final FulfillmentTaskRepository fulfillmentTaskRepository;
    private final IntegrationConnectorRepository integrationConnectorRepository;
    private final IntegrationInboundRecordRepository integrationInboundRecordRepository;
    private final IntegrationImportRunRepository integrationImportRunRepository;
    private final IntegrationReplayRecordRepository integrationReplayRecordRepository;
    private final OperationalDispatchWorkItemRepository operationalDispatchWorkItemRepository;
    private final SystemIncidentService systemIncidentService;
    private final OperationalDispatchQueueService operationalDispatchQueueService;
    private final OperationalMetricsService operationalMetricsService;
    private final TenantContextService tenantContextService;
    private final IntegrationConnectorService integrationConnectorService;
    private final RealtimeService realtimeService;
    private final OperationalAlertHookService operationalAlertHookService;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${synapsecore.build.version:0.0.1-SNAPSHOT}")
    private String buildVersion;

    @Value("${synapsecore.build.commit:local-dev}")
    private String buildCommit;

    @Value("${synapsecore.build.time:untracked}")
    private String buildTime;

    @Value("${RENDER:false}")
    private boolean renderRuntime;

    @Value("${RENDER_GIT_BRANCH:}")
    private String renderGitBranch;

    @Value("${RENDER_SERVICE_NAME:}")
    private String renderServiceName;

    @Value("${RENDER_SERVICE_ID:}")
    private String renderServiceId;

    @Value("${RENDER_INSTANCE_ID:}")
    private String renderInstanceId;

    @Value("${synapsecore.public.app-url:}")
    private String publicAppUrl;

    @Value("${synapsecore.public.api-url:}")
    private String publicApiUrl;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean secureSessionCookies;

    @Value("${synapsecore.queue.dispatch-interval-ms:1500}")
    private long dispatchIntervalMs;

    @Value("${synapsecore.queue.batch-size:16}")
    private int dispatchBatchSize;

    @Value("${synapsecore.system.diagnostics-window-hours:24}")
    private long diagnosticsWindowHours;

    public SystemRuntimeResponse getRuntimeStatus() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.isEmpty()) {
            activeProfiles = Arrays.asList(environment.getDefaultProfiles());
        }
        LivenessState livenessState = applicationAvailability.getLivenessState();
        ReadinessState readinessState = applicationAvailability.getReadinessState();
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        drainOperationalDispatchQueue(tenantCode);

        return new SystemRuntimeResponse(
            applicationName,
            new SystemBuildInfo(
                buildVersion,
                buildCommit,
                buildTime,
                emptyToNull(renderGitBranch),
                renderRuntime ? "render" : "local",
                emptyToNull(renderServiceName),
                emptyToNull(renderServiceId),
                emptyToNull(renderInstanceId)
            ),
            activeProfiles,
            resolveOverallStatus(livenessState, readinessState),
            livenessState.name(),
            readinessState.name(),
            accessProperties.isAllowHeaderFallback(),
            secureSessionCookies,
            corsProperties.getAllowedOrigins(),
            publicAppUrl,
            publicApiUrl,
            buildTelemetrySummary(),
            buildBackboneSummary(),
            buildMetricsSummary(),
            buildDiagnosticsSummary(),
            buildConnectorDiagnostics(),
            Instant.now()
        );
    }

    public TenantRuntimeResponse getTenantRuntimeStatus() {
        LivenessState livenessState = applicationAvailability.getLivenessState();
        ReadinessState readinessState = applicationAvailability.getReadinessState();
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        drainOperationalDispatchQueue(tenantCode);
        SystemBackboneSummary backbone = buildBackboneSummary();

        return new TenantRuntimeResponse(
            applicationName,
            new SystemBuildInfo(buildVersion, buildCommit, buildTime, null, null, null, null, null),
            resolveOverallStatus(livenessState, readinessState),
            livenessState.name(),
            readinessState.name(),
            secureSessionCookies,
            buildTelemetrySummary(),
            new TenantBackboneSummary(
                backbone.realtimeBrokerMode(),
                backbone.realtimeBrokerDetail(),
                backbone.pendingDispatchCount(),
                backbone.failedDispatchCount(),
                backbone.oldestPendingAgeSeconds(),
                backbone.latestProcessedAt()
            ),
            buildMetricsSummary(),
            buildDiagnosticsSummary(),
            buildConnectorDiagnostics(),
            Instant.now()
        );
    }

    public PlatformRuntimeResponse getPlatformRuntimeStatus() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.isEmpty()) {
            activeProfiles = Arrays.asList(environment.getDefaultProfiles());
        }
        LivenessState livenessState = applicationAvailability.getLivenessState();
        ReadinessState readinessState = applicationAvailability.getReadinessState();
        RealtimeBrokerMode brokerMode = realtimeService.brokerMode();

        return new PlatformRuntimeResponse(
            applicationName,
            new SystemBuildInfo(
                buildVersion,
                buildCommit,
                buildTime,
                emptyToNull(renderGitBranch),
                renderRuntime ? "render" : "local",
                emptyToNull(renderServiceName),
                emptyToNull(renderServiceId),
                emptyToNull(renderInstanceId)
            ),
            activeProfiles,
            resolveOverallStatus(livenessState, readinessState),
            livenessState.name(),
            readinessState.name(),
            secureSessionCookies,
            brokerMode.name(),
            brokerMode != RealtimeBrokerMode.SIMPLE_IN_MEMORY,
            brokerMode == RealtimeBrokerMode.REDIS_PUBSUB,
            brokerMode == RealtimeBrokerMode.STOMP_RELAY,
            operationalAlertHookService.isConfigured(),
            operationalDispatchWorkItemRepository.countByStatusIn(
                List.of(OperationalDispatchStatus.PENDING, OperationalDispatchStatus.PROCESSING)),
            operationalDispatchWorkItemRepository.countByStatusIn(List.of(OperationalDispatchStatus.FAILED)),
            Instant.now()
        );
    }

    private String resolveOverallStatus(LivenessState livenessState, ReadinessState readinessState) {
        if (livenessState != LivenessState.CORRECT) {
            return "DOWN";
        }
        if (readinessState == ReadinessState.ACCEPTING_TRAFFIC) {
            return "UP";
        }
        return "DEGRADED";
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private SystemTelemetrySummary buildTelemetrySummary() {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        Instant windowStart = Instant.now().minus(Duration.ofHours(diagnosticsWindowHours));
        long recentImportIssues = integrationImportRunRepository
            .findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(tenantCode).stream()
            .filter(run -> run.getStatus() != IntegrationImportStatus.SUCCESS)
            .filter(run -> run.getCreatedAt() != null && run.getCreatedAt().isAfter(windowStart))
            .count();
        long recentAuditFailures = auditLogRepository
            .findTop20ByTenantCodeIgnoreCaseOrderByCreatedAtDesc(tenantCode).stream()
            .filter(log -> log.getStatus() == AuditStatus.FAILURE)
            .filter(log -> log.getCreatedAt() != null && log.getCreatedAt().isAfter(windowStart))
            .filter(log -> {
                String source = log.getSource();
                return source == null || !source.toLowerCase().contains("integration");
            })
            .count();

        return new SystemTelemetrySummary(
            integrationConnectorRepository.countByTenant_CodeIgnoreCaseAndEnabledFalse(tenantCode),
            integrationReplayRecordRepository.countByTenantCodeIgnoreCaseAndStatusIn(tenantCode, List.of(
                IntegrationReplayStatus.PENDING,
                IntegrationReplayStatus.REPLAY_FAILED,
                IntegrationReplayStatus.DEAD_LETTERED
            )),
            integrationReplayRecordRepository.countByTenantCodeIgnoreCaseAndStatusIn(tenantCode, List.of(
                IntegrationReplayStatus.DEAD_LETTERED
            )),
            recentImportIssues,
            integrationInboundRecordRepository.countByTenantCodeIgnoreCaseAndStatusInAndCreatedAtAfter(
                tenantCode,
                List.of(IntegrationInboundStatus.REJECTED, IntegrationInboundStatus.REPLAY_QUEUED),
                windowStart
            ),
            recentAuditFailures,
            alertScopeService.countVisibleActiveAlerts(tenantCode),
            fulfillmentTaskRepository.countByTenant_CodeIgnoreCaseAndStatusIn(
                tenantCode,
                List.of(FulfillmentStatus.QUEUED, FulfillmentStatus.PICKING, FulfillmentStatus.PACKED)
            ),
            fulfillmentTaskRepository.countByTenant_CodeIgnoreCaseAndStatusIn(
                tenantCode,
                List.of(FulfillmentStatus.DELAYED, FulfillmentStatus.EXCEPTION)
            ),
            operationalDispatchWorkItemRepository.countByTenantCodeIgnoreCaseAndStatusIn(
                tenantCode,
                List.of(OperationalDispatchStatus.PENDING, OperationalDispatchStatus.PROCESSING)
            ),
            operationalDispatchWorkItemRepository.countByTenantCodeIgnoreCaseAndStatusIn(
                tenantCode,
                List.of(OperationalDispatchStatus.FAILED)
            )
        );
    }

    private SystemBackboneSummary buildBackboneSummary() {
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();
        RealtimeBrokerMode brokerMode = realtimeService.brokerMode();
        boolean singleNodeOnly = brokerMode == RealtimeBrokerMode.SIMPLE_IN_MEMORY;
        boolean stompRelayConfigured = brokerMode == RealtimeBrokerMode.STOMP_RELAY;
        boolean redisPubSubConfigured = brokerMode == RealtimeBrokerMode.REDIS_PUBSUB;

        return new SystemBackboneSummary(
            brokerMode.name(),
            describeRealtimeBrokerMode(brokerMode),
            singleNodeOnly,
            stompRelayConfigured,
            !singleNodeOnly,
            redisPubSubConfigured,
            stompRelayConfigured,
            operationalAlertHookService.isConfigured(),
            operationalDispatchWorkItemRepository.countByTenantCodeIgnoreCaseAndStatusIn(
                tenantCode,
                List.of(OperationalDispatchStatus.PENDING, OperationalDispatchStatus.PROCESSING)
            ),
            operationalDispatchWorkItemRepository.countByTenantCodeIgnoreCaseAndStatusIn(
                tenantCode,
                List.of(OperationalDispatchStatus.FAILED)
            ),
            operationalDispatchQueueService.oldestPendingAgeSeconds(tenantCode),
            operationalDispatchWorkItemRepository.findTopByTenantCodeIgnoreCaseAndStatusOrderByProcessedAtDesc(
                    tenantCode,
                    OperationalDispatchStatus.COMPLETED
                )
                .map(workItem -> workItem.getProcessedAt())
                .orElse(null),
            dispatchIntervalMs,
            dispatchBatchSize
        );
    }

    private String describeRealtimeBrokerMode(RealtimeBrokerMode brokerMode) {
        return switch (brokerMode) {
            case SIMPLE_IN_MEMORY -> "Realtime is using the in-process simple broker. This is tenant-scoped but single-node only; switch to REDIS_PUBSUB or STOMP_RELAY before horizontal scaling.";
            case REDIS_PUBSUB -> "Realtime is using tenant-scoped websocket topics with Redis pub/sub fanout so updates propagate across app nodes.";
            case STOMP_RELAY -> "Realtime is routed through the configured external STOMP broker relay for multi-node delivery.";
        };
    }

    private SystemMetricsSummary buildMetricsSummary() {
        return operationalMetricsService.snapshotForTenant(tenantContextService.getCurrentTenantCodeOrDefault());
    }

    private void drainOperationalDispatchQueue(String tenantCode) {
        int drainRetries = 5;
        for (int i = 0; i < drainRetries; i++) {
            try {
                operationalDispatchQueueService.processPendingWork();
                long pending = operationalDispatchWorkItemRepository.countByTenantCodeIgnoreCaseAndStatusIn(
                    tenantCode,
                    List.of(OperationalDispatchStatus.PENDING, OperationalDispatchStatus.PROCESSING)
                );
                if (pending == 0) {
                    return;
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private SystemDiagnosticsSummary buildDiagnosticsSummary() {
        Instant windowStart = Instant.now().minus(Duration.ofHours(diagnosticsWindowHours));
        String tenantCode = tenantContextService.getCurrentTenantCodeOrDefault();

        return new SystemDiagnosticsSummary(
            diagnosticsWindowHours,
            businessEventRepository.countByTenantCodeIgnoreCaseAndCreatedAtAfter(tenantCode, windowStart),
            businessEventRepository.countByTenantCodeIgnoreCaseAndEventTypeInAndCreatedAtAfter(
                tenantCode,
                List.of(BusinessEventType.ORDER_INGESTED),
                windowStart
            ),
            businessEventRepository.countByTenantCodeIgnoreCaseAndEventTypeInAndCreatedAtAfter(
                tenantCode,
                List.of(
                    BusinessEventType.INVENTORY_UPDATED,
                    BusinessEventType.LOW_STOCK_DETECTED,
                    BusinessEventType.RECOMMENDATION_GENERATED
                ),
                windowStart
            ),
            businessEventRepository.countByTenantCodeIgnoreCaseAndEventTypeInAndCreatedAtAfter(
                tenantCode,
                List.of(
                    BusinessEventType.INTEGRATION_IMPORT_PROCESSED,
                    BusinessEventType.INTEGRATION_CONNECTOR_UPDATED,
                    BusinessEventType.INTEGRATION_REPLAY_QUEUED,
                    BusinessEventType.INTEGRATION_REPLAY_FAILED,
                    BusinessEventType.INTEGRATION_REPLAY_COMPLETED
                ),
                windowStart
            ),
            businessEventRepository.countByTenantCodeIgnoreCaseAndEventTypeInAndCreatedAtAfter(
                tenantCode,
                List.of(
                    BusinessEventType.SCENARIO_SAVED,
                    BusinessEventType.SCENARIO_ESCALATION_ADVANCED,
                    BusinessEventType.SCENARIO_SLA_ESCALATED,
                    BusinessEventType.SCENARIO_SLA_ACKNOWLEDGED,
                    BusinessEventType.SCENARIO_APPROVED,
                    BusinessEventType.SCENARIO_REJECTED,
                    BusinessEventType.SCENARIO_RESUBMITTED,
                    BusinessEventType.SCENARIO_ANALYZED,
                    BusinessEventType.SCENARIO_COMPARED,
                    BusinessEventType.SCENARIO_EXECUTED
                ),
                windowStart
            ),
            auditLogRepository.countByTenantCodeIgnoreCaseAndStatusAndCreatedAtAfter(
                tenantCode,
                AuditStatus.FAILURE,
                windowStart),
            systemIncidentService.getActiveIncidents().size(),
            businessEventRepository.findTopByTenantCodeIgnoreCaseOrderByCreatedAtDesc(tenantCode)
                .map(event -> event.getCreatedAt())
                .orElse(null),
            auditLogRepository.findTopByTenantCodeIgnoreCaseAndStatusOrderByCreatedAtDesc(tenantCode, AuditStatus.FAILURE)
                .map(log -> log.getCreatedAt())
                .orElse(null)
        );
    }

    private List<SystemConnectorDiagnosticSummary> buildConnectorDiagnostics() {
        return integrationConnectorService.getConnectors().stream()
            .filter(connector -> connector.healthStatus() != com.synapsecore.integration.dto.IntegrationConnectorHealthStatus.LIVE
                || connector.recentInboundFailureCount() > 0
                || connector.pendingReplayCount() > 0
                || connector.deadLetterCount() > 0)
            .sorted(java.util.Comparator
                .comparingInt((com.synapsecore.integration.dto.IntegrationConnectorResponse connector) -> connector.healthStatus() == com.synapsecore.integration.dto.IntegrationConnectorHealthStatus.OFFLINE ? 0 : 1)
                .thenComparing((com.synapsecore.integration.dto.IntegrationConnectorResponse connector) -> connector.lastFailureAt() == null ? Instant.EPOCH : connector.lastFailureAt(), java.util.Comparator.reverseOrder()))
            .limit(4)
            .map(connector -> new SystemConnectorDiagnosticSummary(
                connector.sourceSystem(),
                connector.type(),
                connector.displayName(),
                connector.healthStatus(),
                connector.healthSummary(),
                connector.lastFailureCode(),
                connector.lastFailureMessage(),
                connector.lastFailureAt(),
                connector.pendingReplayCount(),
                connector.deadLetterCount(),
                connector.oldestPendingReplayAgeSeconds()
            ))
            .toList();
    }
}
