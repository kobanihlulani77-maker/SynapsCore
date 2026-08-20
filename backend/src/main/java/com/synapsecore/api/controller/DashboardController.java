package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.domain.dto.DashboardSnapshotResponse;
import com.synapsecore.domain.dto.DashboardSummaryResponse;
import com.synapsecore.domain.service.DashboardService;
import com.synapsecore.domain.service.OperationalViewService;
import com.synapsecore.access.SynapseAccessRole;
import com.synapsecore.domain.dto.FulfillmentOverviewResponse;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AccessControlService accessControlService;
    private final DashboardService dashboardService;
    private final OperationalViewService operationalViewService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        accessControlService.requireWorkspaceAccess("view dashboard summaries");
        return dashboardService.getSummary();
    }

    @GetMapping("/snapshot")
    public DashboardSnapshotResponse getSnapshot() {
        var actor = accessControlService.requireWorkspaceAccess("view dashboard snapshots");
        DashboardSnapshotResponse snapshot = operationalViewService.getSnapshot();
        var fulfillmentItems = snapshot.fulfillment().activeFulfillments().stream()
            .filter(item -> actor.canAccessWarehouse(item.warehouseCode()))
            .toList();
        var fulfillment = new FulfillmentOverviewResponse(
            fulfillmentItems.stream().filter(item -> item.fulfillmentStatus().name().matches("QUEUED|PICKING|PACKED")).count(),
            fulfillmentItems.stream().filter(item -> Boolean.TRUE.equals(item.backlogRisk())).count(),
            fulfillmentItems.stream().filter(item -> Boolean.TRUE.equals(item.deliveryDelayRisk())).count(),
            fulfillmentItems.stream().filter(item -> Boolean.TRUE.equals(item.anomalyDetected())).count(),
            fulfillmentItems,
            Instant.now()
        );
        boolean integrationAccess = actor.roles().contains(SynapseAccessRole.INTEGRATION_ADMIN)
            || actor.roles().contains(SynapseAccessRole.INTEGRATION_OPERATOR);

        return new DashboardSnapshotResponse(
            snapshot.summary(),
            snapshot.alerts(),
            snapshot.recommendations(),
            snapshot.inventory().stream().filter(item -> actor.canAccessWarehouse(item.warehouseCode())).toList(),
            fulfillment,
            snapshot.recentOrders().stream().filter(order -> actor.canAccessWarehouse(order.warehouseCode())).toList(),
            snapshot.recentEvents(),
            snapshot.auditLogs(),
            snapshot.systemIncidents(),
            integrationAccess
                ? snapshot.integrationConnectors().stream().filter(connector -> actor.canAccessWarehouse(connector.defaultWarehouseCode())).toList()
                : List.of(),
            integrationAccess ? snapshot.integrationImportRuns() : List.of(),
            integrationAccess
                ? snapshot.integrationReplayQueue().stream().filter(replay -> actor.canAccessWarehouse(replay.warehouseCode())).toList()
                : List.of(),
            snapshot.scenarioNotifications().stream().filter(item -> actor.canAccessWarehouse(item.warehouseCode())).toList(),
            snapshot.slaEscalations().stream().filter(item -> actor.canAccessWarehouse(item.warehouseCode())).toList(),
            snapshot.recentScenarios().stream().filter(item -> actor.canAccessWarehouse(item.warehouseCode())).toList(),
            snapshot.generatedAt()
        );
    }
}
