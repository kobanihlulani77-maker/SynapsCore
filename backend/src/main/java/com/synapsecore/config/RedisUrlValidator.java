package com.synapsecore.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class RedisUrlValidator {

    private final Environment environment;

    @PostConstruct
    void validateRedisUrl() {
        String redisUrl = environment.getProperty("spring.data.redis.url");
        if (redisUrl == null || redisUrl.isBlank()) {
            throw new IllegalStateException("SPRING_DATA_REDIS_URL must be set to a non-empty redis:// URL for production.");
        }
    }
}
