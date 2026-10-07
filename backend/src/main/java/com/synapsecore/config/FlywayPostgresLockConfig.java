package com.synapsecore.config;

import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FlywayPostgresLockConfig {

    @Bean
    FlywayConfigurationCustomizer flywayPostgresSessionLock() {
        return configuration -> configuration.getConfigurationExtension(PostgreSQLConfigurationExtension.class)
            .setTransactionalLock(false);
    }
}
