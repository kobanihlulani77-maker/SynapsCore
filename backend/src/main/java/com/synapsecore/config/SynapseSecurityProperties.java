package com.synapsecore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "synapsecore.security")
public class SynapseSecurityProperties {

    private final RateLimitProperties rateLimit = new RateLimitProperties();

    @Getter
    @Setter
    public static class RateLimitProperties {

        private boolean enabled = true;
        private final BucketProperties authLogin = new BucketProperties(30, 60);
        private final BucketProperties authPassword = new BucketProperties(12, 300);
        private final BucketProperties tenantOnboarding = new BucketProperties(8, 300);
        private final BucketProperties accessAdminMutation = new BucketProperties(40, 60);
        private final BucketProperties integrationMutation = new BucketProperties(90, 60);
    }

    @Getter
    @Setter
    public static class BucketProperties {

        private int maxAttempts;
        private int windowSeconds;

        public BucketProperties() {
        }

        public BucketProperties(int maxAttempts, int windowSeconds) {
            this.maxAttempts = maxAttempts;
            this.windowSeconds = windowSeconds;
        }
    }
}
