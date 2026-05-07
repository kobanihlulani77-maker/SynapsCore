package com.synapsecore.security;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
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
        return evaluate(bucketName, principalKey, maxAttempts, windowSeconds).allowed();
    }

    public RateLimitDecision evaluate(String bucketName, String principalKey, int maxAttempts, int windowSeconds) {
        if (maxAttempts < 1 || windowSeconds < 1) {
            return new RateLimitDecision(true, 0, Math.max(maxAttempts, 0), Math.max(maxAttempts, 0), Math.max(windowSeconds, 0));
        }
        String normalizedPrincipal = (principalKey == null || principalKey.isBlank()) ? "unknown" : principalKey.trim();
        String counterKey = "synapsecore:security:rate-limit:" + bucketName + ":" + normalizedPrincipal;

        RateLimitDecision redisDecision = evaluateWithRedis(counterKey, maxAttempts, windowSeconds);
        if (redisDecision != null) {
            return redisDecision;
        }
        return evaluateInMemory(counterKey, maxAttempts, windowSeconds);
    }

    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private RateLimitDecision evaluateWithRedis(String counterKey, int maxAttempts, int windowSeconds) {
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
            Long retryAfterSeconds = redisTemplate.getExpire(counterKey, TimeUnit.SECONDS);
            if (retryAfterSeconds == null || retryAfterSeconds < 0L) {
                redisTemplate.expire(counterKey, Duration.ofSeconds(windowSeconds));
                retryAfterSeconds = (long) windowSeconds;
            }
            int remainingAttempts = Math.max(0, maxAttempts - attempts.intValue());
            return new RateLimitDecision(
                attempts <= maxAttempts,
                attempts.intValue(),
                maxAttempts,
                remainingAttempts,
                Math.max(1L, retryAfterSeconds)
            );
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private RateLimitDecision evaluateInMemory(String counterKey, int maxAttempts, int windowSeconds) {
        long nowMillis = currentTimeMillis();
        WindowCounter counter = inMemoryCounters.computeIfAbsent(counterKey, key -> new WindowCounter(nowMillis));
        synchronized (counter) {
            if (nowMillis - counter.windowStartedAtMillis >= windowSeconds * 1000L) {
                counter.windowStartedAtMillis = nowMillis;
                counter.attempts.set(0);
            }
            int attempts = counter.attempts.incrementAndGet();
            long resetAtMillis = counter.windowStartedAtMillis + (windowSeconds * 1000L);
            long retryAfterSeconds = Math.max(1L, (long) Math.ceil(Math.max(0L, resetAtMillis - nowMillis) / 1000d));
            return new RateLimitDecision(
                attempts <= maxAttempts,
                attempts,
                maxAttempts,
                Math.max(0, maxAttempts - attempts),
                retryAfterSeconds
            );
        }
    }

    public record RateLimitDecision(
        boolean allowed,
        int attempts,
        int maxAttempts,
        int remainingAttempts,
        long retryAfterSeconds
    ) {
    }

    private static final class WindowCounter {
        private long windowStartedAtMillis;
        private final AtomicInteger attempts = new AtomicInteger();

        private WindowCounter(long windowStartedAtMillis) {
            this.windowStartedAtMillis = windowStartedAtMillis;
        }
    }
}
