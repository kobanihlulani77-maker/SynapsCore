package com.synapsecore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "synapsecore.access")
public class SynapseAccessProperties {

    private boolean allowHeaderFallback;

    public boolean isAllowHeaderFallback() {
        return allowHeaderFallback;
    }

    public void setAllowHeaderFallback(boolean allowHeaderFallback) {
        this.allowHeaderFallback = allowHeaderFallback;
    }
}
