package com.crewvy.workspace_service.notification.kafka;

import com.crewvy.common.dto.NotificationMessage;
import com.crewvy.workspace_service.notification.constant.NotificationType;
import com.crewvy.workspace_service.notification.entity.Notification;
import com.crewvy.workspace_service.notification.repository.NotificationRepository;
import com.crewvy.workspace_service.notification.sse.SseAlarmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaMessageListener {
    private final SseAlarmService sseAlarmService;
    private final NotificationRepository notificationRepository;

    public KafkaMessageListener(SseAlarmService sseAlarmService, NotificationRepository notificationRepository) {
        this.sseAlarmService = sseAlarmService;
        this.notificationRepository = notificationRepository;
    }

    @KafkaListener(topics = "notification", groupId = "workspace-notification-group")
    public void listen(NotificationMessage message) {
        // DB 저장
        notificationRepository.save(Notification.builder()
                .receiverId(message.getMemberId())
                .notificationType(NotificationType.valueOf(message.getNotificationType()))
                .content(message.getContent())
                .build());

        // SSE 전송
        sseAlarmService.sendToUser(message.getMemberId(), message.getContent());
    }
}