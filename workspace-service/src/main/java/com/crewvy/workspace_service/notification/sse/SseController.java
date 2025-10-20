package com.crewvy.workspace_service.notification.sse;

import com.crewvy.common.dto.NotificationMessage;
import com.crewvy.common.kafka.KafkaMessagePublisher;
import com.crewvy.common.redis.RedisMessagePublisher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/sse")
public class SseController {

    private final SseAlarmService sseAlarmService;
    private final RedisMessagePublisher messagePublisher;
    private final KafkaMessagePublisher kafkaMessagePublisher;

    public SseController(SseAlarmService sseAlarmService,
                         RedisMessagePublisher messagePublisher,
                         KafkaMessagePublisher kafkaMessagePublisher) {
        this.sseAlarmService = sseAlarmService;
        this.messagePublisher = messagePublisher;
        this.kafkaMessagePublisher = kafkaMessagePublisher;
    }

    /**
     * SSE 구독
     */
    @GetMapping("/subscribe")
    public SseEmitter subscribe(@RequestHeader("X-User-UUID") UUID memberId) {
        return sseAlarmService.subscribe(memberId);
    }

    /**
     * 특정 사용자에게 알림 전송 (Redis Pub/Sub)
     */
    @PostMapping("/send")
    public void sendNotification(@RequestBody NotificationMessage message) throws IOException {
        // Redis Pub/Sub으로 발행
//        messagePublisher.publish("notification-channel", toJson(message));
        kafkaMessagePublisher.publish("notification", message);
    }

    // Object -> JSON 문자열
    private String toJson(NotificationMessage message) throws IOException {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(message);
    }
}