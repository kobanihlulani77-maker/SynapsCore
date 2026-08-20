package com.synapsecore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "synapsecore.platform-owner")
public class SynapsePlatformOwnerProperties {

    private String username = "";
    private String passwordHash = "";
    private String displayName = "SynapseCore Platform Owner";
    private int sessionTimeoutMinutes = 120;

    public boolean isConfigured() {
        return username != null && !username.isBlank()
            && passwordHash != null && !passwordHash.isBlank();
    }
}
