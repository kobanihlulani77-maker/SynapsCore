package com.synapsecore.simulation;

import com.synapsecore.domain.dto.SimulationStatusResponse;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SimulationStateService {

    private static final String ACTIVE_KEY = "synapsecore:simulation:active";
    private static final String UPDATED_AT_KEY = "synapsecore:simulation:updatedAt";

    private final RedisTemplate<String, String> redisTemplate;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicReference<Instant> updatedAt = new AtomicReference<>(Instant.now());

    public boolean activate() {
        if (!active.compareAndSet(false, true)) {
            return false;
        }
        updateState(true);
        return true;
    }

    public boolean deactivate() {
        if (!active.compareAndSet(true, false)) {
            return false;
        }
        updateState(false);
        return true;
    }

    public boolean isActive() {
        return active.get();
    }

    public SimulationStatusResponse getStatus() {
        return new SimulationStatusResponse(active.get(), updatedAt.get());
    }

    private void persistState(boolean activeValue, Instant updatedAtValue) {
        try {
            redisTemplate.opsForValue().set(ACTIVE_KEY, String.valueOf(activeValue));
            redisTemplate.opsForValue().set(UPDATED_AT_KEY, updatedAtValue.toString());
        } catch (Exception ignored) {
        }
    }

    private void updateState(boolean activeValue) {
        Instant now = Instant.now();
        updatedAt.set(now);
        persistState(activeValue, now);
    }
}
