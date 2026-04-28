package com.synapsecore.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("schema-export")
@RequiredArgsConstructor
@Slf4j
public class SchemaExportRunner implements ApplicationRunner {

    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Flyway baseline schema exported to target/flyway-baseline.sql");
        Thread shutdownThread = new Thread(() -> System.exit(org.springframework.boot.SpringApplication.exit(applicationContext, () -> 0)));
        shutdownThread.setDaemon(false);
        shutdownThread.start();
    }
}
