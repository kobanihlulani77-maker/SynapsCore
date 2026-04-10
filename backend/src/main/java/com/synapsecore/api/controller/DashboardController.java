package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.domain.dto.DashboardSnapshotResponse;
import com.synapsecore.domain.dto.DashboardSummaryResponse;
import com.synapsecore.domain.service.DashboardService;
import com.synapsecore.domain.service.OperationalViewService;
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
        accessControlService.requireWorkspaceAccess("view dashboard snapshots");
        return operationalViewService.getSnapshot();
    }
}
