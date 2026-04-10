package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.domain.dto.AlertFeedResponse;
import com.synapsecore.domain.service.OperationalViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AccessControlService accessControlService;
    private final OperationalViewService operationalViewService;

    @GetMapping
    public AlertFeedResponse getAlerts() {
        accessControlService.requireWorkspaceAccess("view operational alerts");
        return operationalViewService.getAlertFeed();
    }
}
