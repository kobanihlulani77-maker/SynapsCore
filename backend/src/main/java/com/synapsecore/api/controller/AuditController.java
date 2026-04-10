package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.audit.AuditLogService;
import com.synapsecore.domain.dto.AuditLogResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AccessControlService accessControlService;
    private final AuditLogService auditLogService;

    @GetMapping("/recent")
    public List<AuditLogResponse> getRecentAuditLogs() {
        accessControlService.requireWorkspaceAccess("view recent audit activity");
        return auditLogService.getRecentAuditLogs();
    }
}
