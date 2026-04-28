package com.synapsecore.config;

import com.synapsecore.realtime.RealtimeBrokerMode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "synapsecore.realtime")
public class SynapseRealtimeProperties {

    private RealtimeBrokerMode brokerMode = RealtimeBrokerMode.SIMPLE_IN_MEMORY;

    private String redisChannel = "synapsecore:realtime";

    private String relayHost = "localhost";

    private int relayPort = 61613;

    private String relayClientLogin = "guest";

    private String relayClientPasscode = "guest";

    private String relaySystemLogin = "guest";

    private String relaySystemPasscode = "guest";

    private String relayVirtualHost;

    private long relaySystemHeartbeatSendMs = 10000L;

    private long relaySystemHeartbeatReceiveMs = 10000L;

    public boolean redisPubSubEnabled() {
        return brokerMode == RealtimeBrokerMode.REDIS_PUBSUB;
    }

    public boolean stompRelayEnabled() {
        return brokerMode == RealtimeBrokerMode.STOMP_RELAY;
    }

    public boolean distributedBrokerEnabled() {
        return brokerMode == RealtimeBrokerMode.REDIS_PUBSUB
            || brokerMode == RealtimeBrokerMode.STOMP_RELAY;
    }
}
