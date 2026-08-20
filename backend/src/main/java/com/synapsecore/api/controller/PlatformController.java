package com.synapsecore.api.controller;

import com.synapsecore.platform.PlatformControlPlaneService;
import com.synapsecore.platform.PlatformOwnerSessionService;
import com.synapsecore.platform.dto.PlatformActivityResponse;
import com.synapsecore.platform.dto.PlatformOverviewResponse;
import com.synapsecore.platform.dto.PlatformRuntimeResponse;
import com.synapsecore.platform.dto.PlatformSessionRequest;
import com.synapsecore.platform.dto.PlatformSessionResponse;
import com.synapsecore.platform.dto.PlatformTenantSummary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformOwnerSessionService platformOwnerSessionService;
    private final PlatformControlPlaneService platformControlPlaneService;

    @GetMapping("/session")
    public PlatformSessionResponse getSession(HttpServletRequest request) {
        return platformOwnerSessionService.getCurrentSession(request.getSession(false));
    }

    @PostMapping("/session/login")
    public PlatformSessionResponse signIn(@Valid @RequestBody PlatformSessionRequest request,
                                          HttpServletRequest httpRequest) {
        return platformOwnerSessionService.signIn(
            httpRequest,
            request.username().trim(),
            request.password()
        );
    }

    @PostMapping("/session/logout")
    public PlatformSessionResponse signOut(HttpServletRequest request) {
        return platformOwnerSessionService.signOut(request);
    }

    @GetMapping("/overview")
    public PlatformOverviewResponse getOverview(HttpServletRequest request) {
        requirePlatformOwner(request, "view the platform overview");
        return platformControlPlaneService.getOverview();
    }

    @GetMapping("/tenants")
    public List<PlatformTenantSummary> getTenants(HttpServletRequest request) {
        requirePlatformOwner(request, "view the platform tenant directory");
        return platformControlPlaneService.getTenants();
    }

    @GetMapping("/runtime")
    public PlatformRuntimeResponse getRuntime(HttpServletRequest request) {
        requirePlatformOwner(request, "view platform runtime metadata");
        return platformControlPlaneService.getRuntime();
    }

    @GetMapping("/activity")
    public List<PlatformActivityResponse> getActivity(HttpServletRequest request) {
        requirePlatformOwner(request, "view platform activity metadata");
        return platformControlPlaneService.getActivity();
    }

    private void requirePlatformOwner(HttpServletRequest request, String actionDescription) {
        platformOwnerSessionService.requirePlatformOwner(request.getSession(false), actionDescription);
    }
}
