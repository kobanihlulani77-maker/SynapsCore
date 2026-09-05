package com.synapsecore.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncExecutionConfig {

    @Value("${synapsecore.async.operational-dispatch.core-pool-size:1}")
    private int corePoolSize = 1;

    @Value("${synapsecore.async.operational-dispatch.max-pool-size:2}")
    private int maxPoolSize = 2;

    @Value("${synapsecore.async.operational-dispatch.queue-capacity:64}")
    private int queueCapacity = 64;

    @Bean(name = "operationalDispatchExecutor")
    public Executor operationalDispatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("synapse-dispatch-");
        int effectiveCorePoolSize = Math.max(corePoolSize, 1);
        executor.setCorePoolSize(effectiveCorePoolSize);
        executor.setMaxPoolSize(Math.max(maxPoolSize, effectiveCorePoolSize));
        executor.setQueueCapacity(Math.max(queueCapacity, 1));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }
}
