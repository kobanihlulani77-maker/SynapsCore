package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.domain.dto.SimulationStatusResponse;
import com.synapsecore.simulation.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final AccessControlService accessControlService;
    private final SimulationService simulationService;

    @PostMapping("/start")
    public SimulationStatusResponse start() {
        accessControlService.requireTenantAdminControl("start simulation mode");
        return simulationService.start();
    }

    @PostMapping("/stop")
    public SimulationStatusResponse stop() {
        accessControlService.requireTenantAdminControl("stop simulation mode");
        return simulationService.stop();
    }

    @GetMapping("/status")
    public SimulationStatusResponse status() {
        accessControlService.requireWorkspaceAccess("view simulation status");
        return simulationService.status();
    }
}
