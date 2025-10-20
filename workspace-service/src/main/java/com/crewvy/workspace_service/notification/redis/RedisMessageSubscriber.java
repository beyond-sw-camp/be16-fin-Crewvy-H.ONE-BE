package com.crewvy.workspace_service.notification.redis;

import com.crewvy.common.dto.NotificationMessage;
import com.crewvy.workspace_service.notification.constant.NotificationType;
import com.crewvy.workspace_service.notification.entity.Notification;
import com.crewvy.workspace_service.notification.repository.NotificationRepository;
import com.crewvy.workspace_service.notification.sse.SseAlarmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class RedisMessageSubscriber implements MessageListener {

    private final SseAlarmService sseAlarmService;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public RedisMessageSubscriber(SseAlarmService sseAlarmService,
                                  NotificationRepository notificationRepository,
                                  ObjectMapper objectMapper) {
        this.sseAlarmService = sseAlarmService;
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String msgStr = new String(message.getBody(), StandardCharsets.UTF_8);

            // Redis 메시지는 JSON 형태 { "memberId": "...", "content": "..." }
            NotificationMessage msg = objectMapper.readValue(msgStr, NotificationMessage.class);

            System.out.println(msg);

            // DB 저장
            Notification notification = Notification.builder()
                    .receiverId(msg.getMemberId())
                    .content(msg.getContent())
                    .notificationType(NotificationType.valueOf(msg.getNotificationType()))
                    .build();
            notificationRepository.save(notification);

            // 특정 사용자에게 SSE 전송
            sseAlarmService.sendToUser(msg.getMemberId(), msg.getContent());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}