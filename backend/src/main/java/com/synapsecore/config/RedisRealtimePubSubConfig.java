package com.synapsecore.config;

import com.synapsecore.realtime.StompRealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "synapsecore.realtime.broker-mode", havingValue = "REDIS_PUBSUB")
public class RedisRealtimePubSubConfig {

    private final SynapseRealtimeProperties realtimeProperties;

    @Bean
    public RedisMessageListenerContainer redisRealtimeMessageListenerContainer(
        RedisConnectionFactory connectionFactory,
        StompRealtimePublisher realtimePublisher
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
            (message, pattern) -> realtimePublisher.acceptRedisMessage(message.getBody()),
            new ChannelTopic(realtimeProperties.getRedisChannel())
        );
        return container;
    }
}
