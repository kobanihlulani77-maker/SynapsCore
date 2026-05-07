package com.synapsecore.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

class SecurityRateLimitServiceTest {

    @Test
    void rollingWindowDoesNotResetWhenAbsoluteClockBoundaryChanges() {
        TestableSecurityRateLimitService service = new TestableSecurityRateLimitService();

        service.setNowMillis(59_000L);
        SecurityRateLimitService.RateLimitDecision firstDecision = service.evaluate("AUTH_LOGIN", "203.0.113.10", 2, 5);
        assertThat(firstDecision.allowed()).isTrue();
        assertThat(firstDecision.remainingAttempts()).isEqualTo(1);

        service.setNowMillis(59_500L);
        SecurityRateLimitService.RateLimitDecision secondDecision = service.evaluate("AUTH_LOGIN", "203.0.113.10", 2, 5);
        assertThat(secondDecision.allowed()).isTrue();
        assertThat(secondDecision.remainingAttempts()).isEqualTo(0);

        service.setNowMillis(60_500L);
        SecurityRateLimitService.RateLimitDecision thirdDecision = service.evaluate("AUTH_LOGIN", "203.0.113.10", 2, 5);
        assertThat(thirdDecision.allowed()).isFalse();
        assertThat(thirdDecision.attempts()).isEqualTo(3);
        assertThat(thirdDecision.remainingAttempts()).isZero();

        service.setNowMillis(64_200L);
        SecurityRateLimitService.RateLimitDecision resetDecision = service.evaluate("AUTH_LOGIN", "203.0.113.10", 2, 5);
        assertThat(resetDecision.allowed()).isTrue();
        assertThat(resetDecision.attempts()).isEqualTo(1);
        assertThat(resetDecision.remainingAttempts()).isEqualTo(1);
    }

    private static final class TestableSecurityRateLimitService extends SecurityRateLimitService {

        private long nowMillis;

        private TestableSecurityRateLimitService() {
            super(new StaticListableBeanFactory().getBeanProvider(StringRedisTemplate.class));
        }

        @Override
        protected long currentTimeMillis() {
            return nowMillis;
        }

        private void setNowMillis(long nowMillis) {
            this.nowMillis = nowMillis;
        }
    }
}
