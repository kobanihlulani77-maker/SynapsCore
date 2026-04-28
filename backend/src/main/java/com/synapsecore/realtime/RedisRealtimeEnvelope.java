package com.synapsecore.realtime;

public record RedisRealtimeEnvelope(
    String originNodeId,
    String destination,
    String payloadJson
) {
}
