package com.synapsecore.security;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityRateLimitService {

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final Map<String, WindowCounter> inMemoryCounters = new ConcurrentHashMap<>();

    public boolean allow(String bucketName, String principalKey, int maxAttempts, int windowSeconds) {
        if (maxAttempts < 1 || windowSeconds < 1) {
            return true;
        }
        String normalizedPrincipal = (principalKey == null || principalKey.isBlank()) ? "unknown" : principalKey.trim();
        long bucketWindow = Math.max(System.currentTimeMillis() / 1000L / windowSeconds, 0L);
        String counterKey = "synapsecore:security:rate-limit:" + bucketName + ":" + normalizedPrincipal + ":" + bucketWindow;

        Boolean redisAllowed = allowWithRedis(counterKey, maxAttempts, windowSeconds);
        if (redisAllowed != null) {
            return redisAllowed;
        }
        return allowInMemory(counterKey, maxAttempts, windowSeconds);
    }

    private Boolean allowWithRedis(String counterKey, int maxAttempts, int windowSeconds) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return null;
        }

        try {
            Long attempts = redisTemplate.opsForValue().increment(counterKey);
            if (attempts == null) {
                return null;
            }
            if (attempts == 1L) {
                redisTemplate.expire(counterKey, Duration.ofSeconds(windowSeconds));
            }
            return attempts <= maxAttempts;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean allowInMemory(String counterKey, int maxAttempts, int windowSeconds) {
        long nowMillis = System.currentTimeMillis();
        WindowCounter counter = inMemoryCounters.computeIfAbsent(counterKey, key -> new WindowCounter(nowMillis));
        synchronized (counter) {
            if (nowMillis - counter.windowStartedAtMillis >= windowSeconds * 1000L) {
                counter.windowStartedAtMillis = nowMillis;
                counter.attempts.set(0);
            }
            return counter.attempts.incrementAndGet() <= maxAttempts;
        }
    }

    private static final class WindowCounter {
        private long windowStartedAtMillis;
        private final AtomicInteger attempts = new AtomicInteger();

        private WindowCounter(long windowStartedAtMillis) {
            this.windowStartedAtMillis = windowStartedAtMillis;
        }
    }
}
