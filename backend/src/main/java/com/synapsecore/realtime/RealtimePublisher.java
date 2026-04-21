package com.synapsecore.realtime;

public interface RealtimePublisher {

    void publish(String destination, Object payload);

    RealtimeBrokerMode brokerMode();
}
