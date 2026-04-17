package com.synapsecore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "synapsecore.bootstrap")
public class SynapseBootstrapProperties {

    private String initialToken = "";
    private String platformAdminToken = "";

    public String getInitialToken() {
        return initialToken;
    }

    public void setInitialToken(String initialToken) {
        this.initialToken = initialToken;
    }

    public boolean hasInitialToken() {
        return initialToken != null && !initialToken.isBlank();
    }

    public String getPlatformAdminToken() {
        return platformAdminToken;
    }

    public void setPlatformAdminToken(String platformAdminToken) {
        this.platformAdminToken = platformAdminToken;
    }

    public boolean hasPlatformAdminToken() {
        return platformAdminToken != null && !platformAdminToken.isBlank();
    }
}
