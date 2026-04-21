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

    private String relayHost = "localhost";

    private int relayPort = 61613;

    private String relayClientLogin = "guest";

    private String relayClientPasscode = "guest";

    private String relaySystemLogin = "guest";

    private String relaySystemPasscode = "guest";

    private String relayVirtualHost;

    private long relaySystemHeartbeatSendMs = 10000L;

    private long relaySystemHeartbeatReceiveMs = 10000L;

    public boolean externalBrokerEnabled() {
        return brokerMode == RealtimeBrokerMode.EXTERNAL_BROKER;
    }
}
