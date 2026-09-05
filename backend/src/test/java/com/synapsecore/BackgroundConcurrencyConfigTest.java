package com.synapsecore;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsecore.config.AsyncExecutionConfig;
import com.synapsecore.config.SchedulingConfig;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class BackgroundConcurrencyConfigTest {

    @Test
    void scheduledWorkUsesOneSharedWorkerByDefault() {
        SchedulingConfig configuration = new SchedulingConfig();
        ThreadPoolTaskScheduler scheduler = configuration.synapseScheduledTaskScheduler();

        try {
            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    void operationalDispatchUsesASeparateBoundedWorkerBudgetByDefault() {
        AsyncExecutionConfig configuration = new AsyncExecutionConfig();
        Executor executor = configuration.operationalDispatchExecutor();
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;

        try {
            assertThat(taskExecutor.getCorePoolSize()).isEqualTo(1);
            assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(2);
        } finally {
            taskExecutor.shutdown();
        }
    }
}
