package com.synapsecore.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(0)
@Profile("!prod")
public class DataInitializer implements CommandLineRunner {

    private final SeedService seedService;

    @Override
    public void run(String... args) {
        seedService.seedIfEmpty();
    }
}
