package com.crewvy.workspace_service.notification.service;

import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.notification.constant.NotificationType;
import com.crewvy.workspace_service.notification.entity.NotificationSetting;
import com.crewvy.workspace_service.notification.repository.NotificationSettingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationSettingService {
    private final NotificationSettingRepository settingRepository;

//    알림 설정 여부 확인
    public boolean settingCheck(UUID memberId, NotificationType type) {
        return settingRepository.findByMemberIdAndNotificationType(memberId, type)
                .map(NotificationSetting::getIsActive)
                .orElse(Bool.TRUE).toBoolean();
    }
}
