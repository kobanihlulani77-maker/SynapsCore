package com.synapsecore.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.platform.PlatformMetadataChangedEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlatformRealtimeNotificationServiceTest {

    @Test
    void publishesOnlySafePlatformActivityChangedMetadata() {
        RecordingRealtimePublisher publisher = new RecordingRealtimePublisher();
        PlatformRealtimeNotificationService service = new PlatformRealtimeNotificationService(publisher);
        Instant occurredAt = Instant.parse("2026-08-31T10:15:30Z");

        service.publishPlatformChange(new PlatformMetadataChangedEvent(occurredAt));

        assertThat(publisher.destinations)
            .containsExactly(PlatformRealtimeNotificationService.PLATFORM_ACTIVITY_CHANGED_DESTINATION);
        assertThat(publisher.payloads).singleElement()
            .extracting("type", "occurredAt", "scope")
            .containsExactly("PLATFORM_ACTIVITY_CHANGED", occurredAt, "PLATFORM");
    }

    @Test
    void deliveryFailureDoesNotEscapeTheNotificationBoundary() {
        RealtimePublisher publisher = new RealtimePublisher() {
            @Override
            public void publish(String destination, Object payload) {
                throw new IllegalStateException("broker unavailable");
            }

            @Override
            public RealtimeBrokerMode brokerMode() {
                return RealtimeBrokerMode.SIMPLE_IN_MEMORY;
            }
        };

        PlatformRealtimeNotificationService service = new PlatformRealtimeNotificationService(publisher);

        org.assertj.core.api.Assertions.assertThatCode(() -> service.publishPlatformChange(
            new PlatformMetadataChangedEvent(Instant.now())
        )).doesNotThrowAnyException();
    }

    private static final class RecordingRealtimePublisher implements RealtimePublisher {

        private final List<String> destinations = new ArrayList<>();
        private final List<Object> payloads = new ArrayList<>();

        @Override
        public void publish(String destination, Object payload) {
            destinations.add(destination);
            payloads.add(payload);
        }

        @Override
        public RealtimeBrokerMode brokerMode() {
            return RealtimeBrokerMode.SIMPLE_IN_MEMORY;
        }
    }
}
