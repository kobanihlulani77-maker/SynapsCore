package com.synapsecore.bootstrap;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "synapsecore.bootstrap.schema-only", havingValue = "true")
public class SchemaBootstrapExitRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaBootstrapExitRunner.class);

    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(String... args) {
        log.info("Schema bootstrap completed. Exiting bootstrap container so the validated backend can start.");
        Thread exitThread = new Thread(() -> {
            int exitCode = SpringApplication.exit(applicationContext, () -> 0);
            System.exit(exitCode);
        }, "synapse-schema-bootstrap-exit");
        exitThread.setDaemon(false);
        exitThread.start();
    }
}
