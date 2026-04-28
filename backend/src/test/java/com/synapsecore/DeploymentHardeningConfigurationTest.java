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
        assertThat(renderConfig).contains("value: REDIS_PUBSUB");
        assertThat(renderConfig).contains("value: validate");
        assertThat(renderConfig).doesNotContain("value: SIMPLE_IN_MEMORY");
        assertThat(renderConfig).doesNotContain("value: update");
    }
}
