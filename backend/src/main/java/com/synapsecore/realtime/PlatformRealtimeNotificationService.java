package com.synapsecore.realtime;

import com.synapsecore.platform.PlatformActivityChangedNotification;
import com.synapsecore.platform.PlatformMetadataChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformRealtimeNotificationService {

    public static final String PLATFORM_ACTIVITY_CHANGED_DESTINATION = "/topic/platform/activity.changed";

    private final RealtimePublisher realtimePublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publishPlatformChange(PlatformMetadataChangedEvent event) {
        try {
            realtimePublisher.publish(
                PLATFORM_ACTIVITY_CHANGED_DESTINATION,
                new PlatformActivityChangedNotification(
                    "PLATFORM_ACTIVITY_CHANGED",
                    event.occurredAt(),
                    "PLATFORM"
                )
            );
        } catch (RuntimeException exception) {
            // Durable audit/business evidence remains authoritative when delivery is unavailable.
            log.warn("Platform metadata notification could not be delivered after commit: {}", exception.getMessage());
        }
    }
}
