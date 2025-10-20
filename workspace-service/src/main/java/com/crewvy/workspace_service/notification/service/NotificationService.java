package com.crewvy.workspace_service.notification.service;

import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.notification.dto.response.NotificationResDto;
import com.crewvy.workspace_service.notification.entity.Notification;
import com.crewvy.workspace_service.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;

//    내 알림 조회
    @Transactional(readOnly = true)
    public List<NotificationResDto> getMyAlarm(UUID memberId) {
        List<Notification> notificationList = notificationRepository.
                findByReceiverIdAndIsDeleted(memberId, Bool.FALSE);

        return notificationList.stream()
                .map(notification -> NotificationResDto.builder()
                        .notificationId(notification.getId())
                        .type(notification.getNotificationType())
                        .contents(notification.getContent())
                        .createAt(notification.getCreatedAt())
                        .build())
                .toList();
    }

    public void readAlarm(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 알림입니다."));

        notification.readNotification();
    }
}
