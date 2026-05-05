package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DeploymentHardeningConfigurationTest {

    @Test
    void productionProfileValidatesSchemaInsteadOfUpdatingIt() throws IOException {
        String prodConfig = Files.readString(Path.of("src/main/resources/application-prod.yml"));
        assertThat(prodConfig).contains("ddl-auto: validate");
        assertThat(prodConfig).doesNotContain("ddl-auto: update");
    }

    @Test
    void renderDeploymentUsesDistributedRealtimeAndValidateMode() throws IOException {
        String renderConfig = Files.readString(Path.of("..", "render.yaml"));
        assertThat(renderConfig).contains("healthCheckPath: /actuator/health/liveness");
        assertThat(renderConfig).contains("key: SPRING_SESSION_REDIS_NAMESPACE");
        assertThat(renderConfig).contains("value: synapsecore:sessions");
        assertThat(renderConfig).contains("value: REDIS_PUBSUB");
        assertThat(renderConfig).contains("value: validate");
        assertThat(renderConfig).doesNotContain("value: SIMPLE_IN_MEMORY");
        assertThat(renderConfig).doesNotContain("value: update");
    }

    @Test
    void productionProfileStoresSessionsInRedisAndSeparatesReadinessFromLiveness() throws IOException {
        String prodConfig = Files.readString(Path.of("src/main/resources/application-prod.yml"));
        String baseConfig = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(prodConfig).contains("store-type: redis");
        assertThat(prodConfig).contains("namespace: ${SPRING_SESSION_REDIS_NAMESPACE:synapsecore:sessions}");
        assertThat(baseConfig).contains("include: livenessState,ping");
        assertThat(baseConfig).contains("include: readinessState,db,redis,ping");
    }
}
