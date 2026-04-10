package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.domain.dto.RecommendationResponse;
import com.synapsecore.domain.service.OperationalViewService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final AccessControlService accessControlService;
    private final OperationalViewService operationalViewService;

    @GetMapping
    public List<RecommendationResponse> getRecommendations() {
        accessControlService.requireWorkspaceAccess("view operational recommendations");
        return operationalViewService.getRecommendations();
    }
}
