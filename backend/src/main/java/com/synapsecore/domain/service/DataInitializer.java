package com.synapsecore.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(0)
public class DataInitializer implements CommandLineRunner {

    private final SeedService seedService;

    @Override
    public void run(String... args) {
        seedService.seedIfEmpty();
    }
}
