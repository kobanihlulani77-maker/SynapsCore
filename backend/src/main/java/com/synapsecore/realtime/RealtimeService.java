package com.synapsecore.realtime;

import com.synapsecore.domain.service.DashboardService;
import com.synapsecore.domain.service.OperationalViewService;
import com.synapsecore.tenant.TenantContextService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeService {

    private final RealtimePublisher realtimePublisher;
    private final OperationalViewService operationalViewService;
    private final DashboardService dashboardService;
    private final TenantContextService tenantContextService;

    public void broadcastOperationalUpdates() {
        broadcastOperationalUpdates(tenantContextService.getCurrentTenantCodeOrDefault());
    }

    public void broadcastIntegrationUpdates() {
        broadcastIntegrationUpdates(tenantContextService.getCurrentTenantCodeOrDefault());
    }

    public void broadcastOperationalUpdates(String tenantCode) {
        realtimePublisher.publish(topic(tenantCode, "/dashboard.summary"), dashboardService.getSummary());
        realtimePublisher.publish(topic(tenantCode, "/alerts"), operationalViewService.getAlertFeed());
        realtimePublisher.publish(topic(tenantCode, "/recommendations"), operationalViewService.getRecommendations());
        realtimePublisher.publish(topic(tenantCode, "/inventory"), operationalViewService.getInventoryOverview());
        realtimePublisher.publish(topic(tenantCode, "/fulfillment.overview"), operationalViewService.getFulfillmentOverview());
        realtimePublisher.publish(topic(tenantCode, "/orders.recent"), operationalViewService.getRecentOrders());
        realtimePublisher.publish(topic(tenantCode, "/events.recent"), operationalViewService.getRecentEvents());
        realtimePublisher.publish(topic(tenantCode, "/audit.recent"), operationalViewService.getRecentAuditLogs());
        realtimePublisher.publish(topic(tenantCode, "/system.incidents"), operationalViewService.getSystemIncidents());
        realtimePublisher.publish(topic(tenantCode, "/integrations.connectors"), operationalViewService.getIntegrationConnectors());
        realtimePublisher.publish(topic(tenantCode, "/integrations.imports"), operationalViewService.getRecentIntegrationImportRuns());
        realtimePublisher.publish(topic(tenantCode, "/integrations.replay"), operationalViewService.getIntegrationReplayQueue());
        realtimePublisher.publish(topic(tenantCode, "/scenarios.notifications"), operationalViewService.getScenarioNotifications());
        realtimePublisher.publish(topic(tenantCode, "/scenarios.escalated"), operationalViewService.getSlaEscalations());
    }

    public void broadcastIntegrationUpdates(String tenantCode) {
        realtimePublisher.publish(topic(tenantCode, "/events.recent"), operationalViewService.getRecentEvents());
        realtimePublisher.publish(topic(tenantCode, "/audit.recent"), operationalViewService.getRecentAuditLogs());
        realtimePublisher.publish(topic(tenantCode, "/system.incidents"), operationalViewService.getSystemIncidents());
        realtimePublisher.publish(topic(tenantCode, "/integrations.connectors"), operationalViewService.getIntegrationConnectors());
        realtimePublisher.publish(topic(tenantCode, "/integrations.imports"), operationalViewService.getRecentIntegrationImportRuns());
        realtimePublisher.publish(topic(tenantCode, "/integrations.replay"), operationalViewService.getIntegrationReplayQueue());
    }

    public RealtimeBrokerMode brokerMode() {
        return realtimePublisher.brokerMode();
    }

    private String topic(String tenantCode, String suffix) {
        return "/topic/tenant/" + normalizeTenantCode(tenantCode) + suffix;
    }

    private String normalizeTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalArgumentException("Realtime updates require an explicit tenant code.");
        }
        return tenantCode.trim().toUpperCase(Locale.ROOT);
    }
}
