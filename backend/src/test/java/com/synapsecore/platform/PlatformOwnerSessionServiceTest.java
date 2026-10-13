package com.synapsecore.platform;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class PlatformOwnerSessionServiceTest {

    @Test
    void slowSignInLogsOnlyBoundedStageDurations() {
        PlatformOwnerSessionService service = new PlatformOwnerSessionService(null, null, null);
        Logger logger = (Logger) LoggerFactory.getLogger(PlatformOwnerSessionService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            service.logSlowSignIn(0, 500_000_000L, 600_000_000L, 1_999_000_000L);
            assertThat(appender.list).isEmpty();

            service.logSlowSignIn(0, 500_000_000L, 600_000_000L, 2_300_000_000L);
            assertThat(appender.list).hasSize(1);
            String message = appender.list.get(0).getFormattedMessage();
            assertThat(message).contains("totalMs=2300", "credentialsMs=500", "sessionMs=100",
                "auditMs=1700");
            assertThat(message).doesNotContain("username", "password", "sessionId", "requestBody");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
