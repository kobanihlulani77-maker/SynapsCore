package com.synapsecore.api.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceStatusController {

    @GetMapping("/")
    public Map<String, Object> rootStatus() {
        return Map.of(
            "service", "synapsecore-backend",
            "status", "ok",
            "health", Map.of(
                "liveness", "/actuator/health/liveness",
                "readiness", "/actuator/health/readiness",
                "runtime", "/api/system/runtime"
            )
        );
    }
}
