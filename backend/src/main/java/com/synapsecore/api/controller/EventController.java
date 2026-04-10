package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.domain.dto.BusinessEventResponse;
import com.synapsecore.event.BusinessEventQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final AccessControlService accessControlService;
    private final BusinessEventQueryService businessEventQueryService;

    @GetMapping("/recent")
    public List<BusinessEventResponse> getRecentEvents() {
        accessControlService.requireWorkspaceAccess("view recent business events");
        return businessEventQueryService.getRecentEvents();
    }
}
