package com.synapsecore.domain.service;

import com.synapsecore.audit.AuditLogService;
import com.synapsecore.domain.dto.AuditLogResponse;
import com.synapsecore.domain.dto.SystemIncidentResponse;
import com.synapsecore.domain.dto.SystemIncidentSeverity;
import com.synapsecore.domain.dto.SystemIncidentType;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.integration.IntegrationConnectorService;
import com.synapsecore.integration.IntegrationReplayService;
import com.synapsecore.integration.dto.IntegrationConnectorResponse;
import com.synapsecore.integration.dto.IntegrationReplayRecordResponse;
import com.synapsecore.scenario.ScenarioHistoryService;
import com.synapsecore.scenario.dto.ScenarioNotificationResponse;
import com.synapsecore.scenario.dto.ScenarioNotificationType;
import com.synapsecore.tenant.TenantContextService;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemIncidentService {

    private static final int DEFAULT_LIMIT = 8;

    private final AuditLogService auditLogService;
    private final IntegrationConnectorService integrationConnectorService;
    private final IntegrationReplayService integrationReplayService;
    private final ScenarioHistoryService scenarioHistoryService;
    private final OperationalDispatchWorkItemRepository operationalDispatchWorkItemRepository;
    private final TenantContextService tenantContextService;

    @Transactional(readOnly = true)
    public List<SystemIncidentResponse> getActiveIncidents() {
        return Stream.of(
                auditLogService.getRecentAuditLogs().stream()
                    .filter(log -> "FAILURE".equals(log.status().name()))
                    .map(this::toAuditIncident),
                integrationReplayService.getReplayQueue().stream()
                    .map(this::toReplayIncident),
                integrationConnectorService.getConnectors().stream()
                    .filter(connector -> !connector.enabled())
                    .map(this::toConnectorIncident),
                operationalDispatchWorkItemRepository.findTop8ByTenantCodeIgnoreCaseAndStatusOrderByUpdatedAtDesc(
                        tenantContextService.getCurrentTenantCodeOrDefault(),
                        OperationalDispatchStatus.FAILED
                    )
                    .stream()
                    .map(this::toDispatchIncident),
                scenarioHistoryService.getScenarioNotifications().stream()
                    .filter(ScenarioNotificationResponse::actionRequired)
                    .map(this::toScenarioIncident)
            )
            .flatMap(stream -> stream)
            .sorted(Comparator.comparing(SystemIncidentResponse::createdAt).reversed())
            .limit(DEFAULT_LIMIT)
            .toList();
    }

    private SystemIncidentResponse toAuditIncident(AuditLogResponse log) {
        return new SystemIncidentResponse(
            "audit-" + log.id(),
            SystemIncidentType.AUDIT_FAILURE,
            SystemIncidentSeverity.HIGH,
            formatCodeLabel(log.action()),
            log.details(),
            log.source() + " | " + log.actor(),
            true,
            log.createdAt()
        );
    }

    private SystemIncidentResponse toReplayIncident(IntegrationReplayRecordResponse record) {
        SystemIncidentSeverity severity = "REPLAY_FAILED".equals(record.status().name())
            ? SystemIncidentSeverity.HIGH
            : SystemIncidentSeverity.MEDIUM;
        return new SystemIncidentResponse(
            "replay-" + record.id(),
            SystemIncidentType.REPLAY_BACKLOG,
            severity,
            "Replay " + record.externalOrderId(),
            record.failureMessage(),
            record.sourceSystem() + " | " + formatCodeLabel(record.connectorType().name()),
            true,
            record.createdAt()
        );
    }

    private SystemIncidentResponse toConnectorIncident(IntegrationConnectorResponse connector) {
        return new SystemIncidentResponse(
            "connector-" + connector.id(),
            SystemIncidentType.CONNECTOR_DISABLED,
            SystemIncidentSeverity.MEDIUM,
            connector.displayName() + " disabled",
            connector.notes() == null || connector.notes().isBlank()
                ? "Connector is currently paused and will not ingest new activity."
                : connector.notes(),
            connector.sourceSystem() + " | " + formatCodeLabel(connector.type().name()),
            true,
            connector.updatedAt() == null ? connector.createdAt() : connector.updatedAt()
        );
    }

    private SystemIncidentResponse toDispatchIncident(com.synapsecore.domain.entity.OperationalDispatchWorkItem workItem) {
        return new SystemIncidentResponse(
            "dispatch-" + workItem.getId(),
            SystemIncidentType.BACKBONE_DISPATCH_FAILURE,
            SystemIncidentSeverity.HIGH,
            "Dispatch queue failure for " + formatCodeLabel(workItem.getUpdateType().name()),
            workItem.getLastError() == null || workItem.getLastError().isBlank()
                ? "Queued operational fan-out failed and is waiting for investigation."
                : workItem.getLastError(),
            workItem.getSource() + " | request " + workItem.getRequestId(),
            true,
            workItem.getUpdatedAt()
        );
    }

    private SystemIncidentResponse toScenarioIncident(ScenarioNotificationResponse notification) {
        SystemIncidentSeverity severity = notification.type() == ScenarioNotificationType.SLA_ESCALATED
            ? SystemIncidentSeverity.CRITICAL
            : SystemIncidentSeverity.MEDIUM;
        return new SystemIncidentResponse(
            "notification-" + notification.scenarioRunId() + "-" + notification.type(),
            SystemIncidentType.CONTROL_NOTICE,
            severity,
            notification.title(),
            notification.message(),
            (notification.warehouseCode() == null ? "Control center" : notification.warehouseCode())
                + " | "
                + (notification.actor() == null ? "Assigned operator" : notification.actor()),
            notification.actionRequired(),
            notification.createdAt()
        );
    }

    private String formatCodeLabel(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        return String.join(" ",
            java.util.Arrays.stream(value.toLowerCase().replace('-', '_').split("_"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .toList());
    }
}
