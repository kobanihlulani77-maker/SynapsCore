package com.synapsecore.realtime;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.service.DashboardService;
import com.synapsecore.domain.service.OperationalViewService;
import com.synapsecore.tenant.TenantContextService;
import java.time.Instant;
import java.util.Map;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeService {

    private final RealtimePublisher realtimePublisher;
    private final OperationalViewService operationalViewService;
    private final DashboardService dashboardService;
    private final RequestTraceContext requestTraceContext;
    private final TenantContextService tenantContextService;

    public void broadcastOperationalUpdates() {
        broadcastOperationalUpdates(tenantContextService.getCurrentTenantCodeOrDefault());
    }

    public void broadcastIntegrationUpdates() {
        broadcastIntegrationUpdates(tenantContextService.getCurrentTenantCodeOrDefault());
    }

    public void broadcastOperationalUpdates(String tenantCode) {
        String normalizedTenantCode = normalizeTenantCode(tenantCode);
        withTenantContext(normalizedTenantCode, () -> {
            realtimePublisher.publish(topic(normalizedTenantCode, "/dashboard.summary"), dashboardService.getSummary());
            realtimePublisher.publish(topic(normalizedTenantCode, "/alerts"), operationalViewService.getAlertFeed());
            realtimePublisher.publish(topic(normalizedTenantCode, "/alerts.changed"), Map.of("changedAt", Instant.now()));
            realtimePublisher.publish(topic(normalizedTenantCode, "/recommendations"), operationalViewService.getRecommendations());
            realtimePublisher.publish(topic(normalizedTenantCode, "/recommendations.changed"), Map.of("changedAt", Instant.now()));
            realtimePublisher.publish(topic(normalizedTenantCode, "/inventory"), operationalViewService.getInventoryOverview());
            realtimePublisher.publish(topic(normalizedTenantCode, "/fulfillment.overview"), operationalViewService.getFulfillmentOverview());
            realtimePublisher.publish(topic(normalizedTenantCode, "/orders.recent"), operationalViewService.getRecentOrders());
            realtimePublisher.publish(topic(normalizedTenantCode, "/events.recent"), operationalViewService.getRecentEvents());
            realtimePublisher.publish(topic(normalizedTenantCode, "/audit.recent"), operationalViewService.getRecentAuditLogs());
            realtimePublisher.publish(topic(normalizedTenantCode, "/system.incidents"), operationalViewService.getSystemIncidents());
            realtimePublisher.publish(topic(normalizedTenantCode, "/integrations.connectors"), operationalViewService.getIntegrationConnectors());
            realtimePublisher.publish(topic(normalizedTenantCode, "/integrations.imports"), operationalViewService.getRecentIntegrationImportRuns());
            realtimePublisher.publish(topic(normalizedTenantCode, "/integrations.replay"), operationalViewService.getIntegrationReplayQueue());
            realtimePublisher.publish(topic(normalizedTenantCode, "/integrations.changed"), Map.of("changedAt", Instant.now()));
            realtimePublisher.publish(topic(normalizedTenantCode, "/scenarios.notifications"), operationalViewService.getScenarioNotifications());
            realtimePublisher.publish(topic(normalizedTenantCode, "/scenarios.escalated"), operationalViewService.getSlaEscalations());
        });
    }

    public void broadcastIntegrationUpdates(String tenantCode) {
        String normalizedTenantCode = normalizeTenantCode(tenantCode);
        withTenantContext(normalizedTenantCode, () -> {
            realtimePublisher.publish(topic(normalizedTenantCode, "/events.recent"), operationalViewService.getRecentEvents());
            realtimePublisher.publish(topic(normalizedTenantCode, "/audit.recent"), operationalViewService.getRecentAuditLogs());
            realtimePublisher.publish(topic(normalizedTenantCode, "/system.incidents"), operationalViewService.getSystemIncidents());
            realtimePublisher.publish(topic(normalizedTenantCode, "/alerts.changed"), Map.of("changedAt", Instant.now()));
            realtimePublisher.publish(topic(normalizedTenantCode, "/integrations.connectors"), operationalViewService.getIntegrationConnectors());
            realtimePublisher.publish(topic(normalizedTenantCode, "/integrations.imports"), operationalViewService.getRecentIntegrationImportRuns());
            realtimePublisher.publish(topic(normalizedTenantCode, "/integrations.replay"), operationalViewService.getIntegrationReplayQueue());
            realtimePublisher.publish(topic(normalizedTenantCode, "/integrations.changed"), Map.of("changedAt", Instant.now()));
        });
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

    private void withTenantContext(String tenantCode, Runnable action) {
        if (requestTraceContext == null) {
            action.run();
            return;
        }

        String previousTenant = requestTraceContext.getCurrentTenant().orElse(null);
        requestTraceContext.setCurrentTenant(tenantCode);
        try {
            action.run();
        } finally {
            if (previousTenant != null && !previousTenant.isBlank()) {
                requestTraceContext.setCurrentTenant(previousTenant);
            } else {
                requestTraceContext.clearCurrentTenant();
            }
        }
    }
}
