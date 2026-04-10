package com.synapsecore.realtime;

import com.synapsecore.domain.service.DashboardService;
import com.synapsecore.domain.service.OperationalViewService;
import com.synapsecore.simulation.SimulationStateService;
import com.synapsecore.tenant.TenantContextService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeService {

    private final SimpMessagingTemplate messagingTemplate;
    private final OperationalViewService operationalViewService;
    private final DashboardService dashboardService;
    private final SimulationStateService simulationStateService;
    private final TenantContextService tenantContextService;

    public void broadcastOperationalUpdates() {
        broadcastOperationalUpdates(tenantContextService.getCurrentTenantCodeOrDefault());
    }

    public void broadcastIntegrationUpdates() {
        broadcastIntegrationUpdates(tenantContextService.getCurrentTenantCodeOrDefault());
    }

    public void broadcastOperationalUpdates(String tenantCode) {
        messagingTemplate.convertAndSend(topic(tenantCode, "/dashboard.summary"), dashboardService.getSummary());
        messagingTemplate.convertAndSend(topic(tenantCode, "/alerts"), operationalViewService.getAlertFeed());
        messagingTemplate.convertAndSend(topic(tenantCode, "/recommendations"), operationalViewService.getRecommendations());
        messagingTemplate.convertAndSend(topic(tenantCode, "/inventory"), operationalViewService.getInventoryOverview());
        messagingTemplate.convertAndSend(topic(tenantCode, "/fulfillment.overview"), operationalViewService.getFulfillmentOverview());
        messagingTemplate.convertAndSend(topic(tenantCode, "/orders.recent"), operationalViewService.getRecentOrders());
        messagingTemplate.convertAndSend(topic(tenantCode, "/events.recent"), operationalViewService.getRecentEvents());
        messagingTemplate.convertAndSend(topic(tenantCode, "/audit.recent"), operationalViewService.getRecentAuditLogs());
        messagingTemplate.convertAndSend(topic(tenantCode, "/system.incidents"), operationalViewService.getSystemIncidents());
        messagingTemplate.convertAndSend(topic(tenantCode, "/integrations.connectors"), operationalViewService.getIntegrationConnectors());
        messagingTemplate.convertAndSend(topic(tenantCode, "/integrations.imports"), operationalViewService.getRecentIntegrationImportRuns());
        messagingTemplate.convertAndSend(topic(tenantCode, "/integrations.replay"), operationalViewService.getIntegrationReplayQueue());
        messagingTemplate.convertAndSend(topic(tenantCode, "/scenarios.notifications"), operationalViewService.getScenarioNotifications());
        messagingTemplate.convertAndSend(topic(tenantCode, "/scenarios.escalated"), operationalViewService.getSlaEscalations());
        messagingTemplate.convertAndSend(topic(tenantCode, "/simulation.status"), simulationStateService.getStatus());
    }

    public void broadcastIntegrationUpdates(String tenantCode) {
        messagingTemplate.convertAndSend(topic(tenantCode, "/events.recent"), operationalViewService.getRecentEvents());
        messagingTemplate.convertAndSend(topic(tenantCode, "/audit.recent"), operationalViewService.getRecentAuditLogs());
        messagingTemplate.convertAndSend(topic(tenantCode, "/system.incidents"), operationalViewService.getSystemIncidents());
        messagingTemplate.convertAndSend(topic(tenantCode, "/integrations.connectors"), operationalViewService.getIntegrationConnectors());
        messagingTemplate.convertAndSend(topic(tenantCode, "/integrations.imports"), operationalViewService.getRecentIntegrationImportRuns());
        messagingTemplate.convertAndSend(topic(tenantCode, "/integrations.replay"), operationalViewService.getIntegrationReplayQueue());
    }

    private String topic(String tenantCode, String suffix) {
        return "/topic/tenant/" + normalizeTenantCode(tenantCode) + suffix;
    }

    private String normalizeTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return tenantContextService.getDefaultTenantCode();
        }
        return tenantCode.trim().toUpperCase(Locale.ROOT);
    }
}
