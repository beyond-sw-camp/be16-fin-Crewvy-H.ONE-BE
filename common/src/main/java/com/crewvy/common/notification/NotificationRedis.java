package com.crewvy.common.notification;

import com.crewvy.common.dto.NotificationMessage;
import com.crewvy.common.redis.RedisChannel;
import com.crewvy.common.redis.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class NotificationRedis {

    private final RedisMessagePublisher messagePublisher;

    public void sendNotification(NotificationMessage message) throws IOException {
        // Redis Pub/Sub으로 발행
        messagePublisher.publish(RedisChannel.NOTIFICATION_CHANNEL, toJson(message));
    }

    // Object -> JSON 문자열
    private String toJson(NotificationMessage message) throws IOException {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(message);
    }
}
