package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.domain.dto.TenantOperationalPolicyRequest;
import com.synapsecore.domain.dto.TenantOperationalPolicyResponse;
import com.synapsecore.domain.service.TenantOperationalPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/policy")
@RequiredArgsConstructor
public class OperationalPolicyController {

    private final AccessControlService accessControlService;
    private final TenantOperationalPolicyService tenantOperationalPolicyService;

    @GetMapping
    public TenantOperationalPolicyResponse getPolicy() {
        accessControlService.requireWorkspaceAccess("view operational policy");
        return tenantOperationalPolicyService.getCurrentPolicyResponse();
    }

    @PutMapping
    public TenantOperationalPolicyResponse updatePolicy(@Valid @RequestBody TenantOperationalPolicyRequest request) {
        accessControlService.requireTenantAdmin("update operational policy");
        return tenantOperationalPolicyService.updateCurrentPolicy(request);
    }
}
