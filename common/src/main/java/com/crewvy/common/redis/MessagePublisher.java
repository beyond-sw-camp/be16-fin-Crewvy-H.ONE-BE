package com.crewvy.common.redis;

public interface MessagePublisher {
    void publish(String topic, String message);
}