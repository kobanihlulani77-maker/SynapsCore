package com.synapsecore.api.controller;

import com.synapsecore.domain.dto.SeedResetResponse;
import com.synapsecore.domain.service.SeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Profile({"dev", "local", "test"})
public class DevToolsController {

    private final SeedService seedService;

    @PostMapping("/reseed")
    public SeedResetResponse reseedStarterData() {
        return seedService.reseedStarterData();
    }
}
