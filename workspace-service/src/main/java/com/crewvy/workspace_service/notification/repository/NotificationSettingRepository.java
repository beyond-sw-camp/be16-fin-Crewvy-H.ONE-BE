package com.crewvy.workspace_service.notification.repository;

import com.crewvy.workspace_service.notification.constant.NotificationType;
import com.crewvy.workspace_service.notification.dto.response.NotificationSettingResDto;
import com.crewvy.workspace_service.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, UUID> {
    List<NotificationSetting> findByMemberId(UUID memberId);

    Optional<NotificationSetting> findByMemberIdAndNotificationType(UUID memberId, NotificationType type);
}
