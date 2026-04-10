package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.domain.dto.SystemIncidentResponse;
import com.synapsecore.domain.dto.SystemRuntimeResponse;
import com.synapsecore.domain.service.SystemIncidentService;
import com.synapsecore.domain.service.SystemRuntimeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final AccessControlService accessControlService;
    private final SystemRuntimeService systemRuntimeService;
    private final SystemIncidentService systemIncidentService;

    @GetMapping("/runtime")
    public SystemRuntimeResponse getRuntimeStatus() {
        accessControlService.requireWorkspaceAccess("view runtime status");
        return systemRuntimeService.getRuntimeStatus();
    }

    @GetMapping("/incidents")
    public List<SystemIncidentResponse> getActiveIncidents() {
        accessControlService.requireWorkspaceAccess("view active incidents");
        return systemIncidentService.getActiveIncidents();
    }
}
