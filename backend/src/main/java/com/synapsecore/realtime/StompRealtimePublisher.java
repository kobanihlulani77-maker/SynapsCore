package com.synapsecore.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsecore.config.SynapseRealtimeProperties;
import com.synapsecore.observability.OperationalMetricsService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StompRealtimePublisher implements RealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final SynapseRealtimeProperties realtimeProperties;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final OperationalMetricsService operationalMetricsService;
    private final String nodeId = UUID.randomUUID().toString();

    @Autowired
    public StompRealtimePublisher(SimpMessagingTemplate messagingTemplate,
                                  SynapseRealtimeProperties realtimeProperties,
                                  RedisTemplate<String, String> redisTemplate,
                                  ObjectMapper objectMapper,
                                  OperationalMetricsService operationalMetricsService) {
        this.messagingTemplate = messagingTemplate;
        this.realtimeProperties = realtimeProperties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.operationalMetricsService = operationalMetricsService;
    }

    public StompRealtimePublisher(SimpMessagingTemplate messagingTemplate,
                                  SynapseRealtimeProperties realtimeProperties) {
        this(messagingTemplate, realtimeProperties, null, new ObjectMapper().findAndRegisterModules(), null);
    }

    @Override
    public void publish(String destination, Object payload) {
        publishLocally(destination, payload);
        recordRealtimePublish(destination, "LOCAL");
        if (!realtimeProperties.redisPubSubEnabled()) {
            return;
        }
        if (redisTemplate == null) {
            throw new IllegalStateException("Redis-backed realtime is enabled, but no RedisTemplate is available.");
        }
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String envelopeJson = objectMapper.writeValueAsString(new RedisRealtimeEnvelope(nodeId, destination, payloadJson));
            redisTemplate.convertAndSend(realtimeProperties.getRedisChannel(), envelopeJson);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Realtime payload could not be serialized for Redis fanout.", exception);
        }
    }

    public void acceptRedisMessage(byte[] body) {
        if (body == null || body.length == 0) {
            return;
        }
        try {
            RedisRealtimeEnvelope envelope = objectMapper.readValue(body, RedisRealtimeEnvelope.class);
            if (nodeId.equals(envelope.originNodeId())) {
                return;
            }
            JsonNode payload = objectMapper.readTree(envelope.payloadJson());
            publishLocally(envelope.destination(), payload);
            recordRealtimePublish(envelope.destination(), "DISTRIBUTED_FANOUT");
        } catch (Exception exception) {
            String preview = new String(body, StandardCharsets.UTF_8);
            log.warn("Ignoring malformed Redis realtime envelope: {}", preview, exception);
        }
    }

    @Override
    public RealtimeBrokerMode brokerMode() {
        return realtimeProperties.getBrokerMode();
    }

    private void publishLocally(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }

    private void recordRealtimePublish(String destination, String deliveryMode) {
        if (operationalMetricsService == null) {
            return;
        }
        operationalMetricsService.recordRealtimePublish(resolveTenantCode(destination), brokerMode(), deliveryMode);
    }

    private String resolveTenantCode(String destination) {
        if (destination == null || destination.isBlank()) {
            return null;
        }
        String[] segments = destination.split("/");
        for (int index = 0; index < segments.length - 1; index++) {
            if ("tenant".equalsIgnoreCase(segments[index]) && index + 1 < segments.length) {
                return segments[index + 1].trim().toUpperCase(Locale.ROOT);
            }
        }
        return null;
    }
}
