package com.synapsecore.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public class RedisUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles("prod")) {
            return;
        }
        String redisUrl = environment.getProperty("spring.data.redis.url");
        if (redisUrl == null || redisUrl.isBlank()) {
            throw new IllegalStateException(
                "SPRING_DATA_REDIS_URL must be set to a non-empty redis:// URL for production."
            );
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
