package com.synapsecore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "synapsecore.observability")
public class SynapseObservabilityProperties {

    private final AlertHookProperties alertHook = new AlertHookProperties();

    @Getter
    @Setter
    public static class AlertHookProperties {

        private boolean enabled = false;
        private String webhookUrl = "";

        public boolean isConfigured() {
            return enabled && webhookUrl != null && !webhookUrl.isBlank();
        }
    }
}
