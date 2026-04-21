package com.synapsecore.realtime;

import com.synapsecore.config.SynapseRealtimeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompRealtimePublisher implements RealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final SynapseRealtimeProperties realtimeProperties;

    @Override
    public void publish(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }

    @Override
    public RealtimeBrokerMode brokerMode() {
        return realtimeProperties.getBrokerMode();
    }
}
