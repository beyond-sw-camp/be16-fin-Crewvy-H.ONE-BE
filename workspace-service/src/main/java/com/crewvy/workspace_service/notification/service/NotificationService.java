package com.crewvy.workspace_service.notification.service;

import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.notification.constant.NotificationType;
import com.crewvy.workspace_service.notification.dto.request.NotificationSettingReqDto;
import com.crewvy.workspace_service.notification.dto.response.NotificationResDto;
import com.crewvy.workspace_service.notification.dto.response.NotificationSettingResDto;
import com.crewvy.workspace_service.notification.entity.Notification;
import com.crewvy.workspace_service.notification.entity.NotificationSetting;
import com.crewvy.workspace_service.notification.repository.NotificationRepository;
import com.crewvy.workspace_service.notification.repository.NotificationSettingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;

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

//    알림 읽음처리
    public void readAlarm(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 알림입니다."));

        notification.readNotification();
    }

//    계정 생성시 알림 생성
    public void settingNotification(UUID memberId) {
        List<NotificationType> allTypes = Arrays.asList(NotificationType.values());
        List<NotificationSetting> settingList = new ArrayList<>();
        for(NotificationType type : allTypes) {
            NotificationSetting setting = NotificationSetting.builder()
                    .notificationType(type)
                    .memberId(memberId)
                    .isActive(Bool.TRUE)
                    .build();
            settingList.add(setting);
        }
        notificationSettingRepository.saveAll(settingList);
    }

    @Transactional(readOnly = true)
    public List<NotificationSettingResDto> findMySetting(UUID memberId) {
        return notificationSettingRepository.findByMemberId(memberId).stream()
                .map(NotificationSettingResDto::from)
                .toList();
    }

    public void updateSetting(List<NotificationSettingReqDto> dtoList) {

        // 1. DTO 리스트에서 모든 settingId를 한 번에 추출합니다.
        List<UUID> settingIds = dtoList.stream()
                .map(NotificationSettingReqDto::getSettingId)
                .collect(Collectors.toList());

        // 2. ID 리스트를 사용해 "단 1번의 SELECT"로 모든 엔티티를 조회하고,
        //    빠른 조회를 위해 Map<ID, 엔티티> 형태로 변환합니다.
        Map<UUID, NotificationSetting> settingMap = notificationSettingRepository.findAllById(settingIds)
                .stream()
                .collect(Collectors.toMap(NotificationSetting::getId, Function.identity()));

        // 3. DTO 리스트를 순회하며 Map에서 엔티티를 찾아 값을 변경합니다.
        dtoList.forEach(dto -> {
            NotificationSetting setting = settingMap.get(dto.getSettingId());

            // 4. Map에 해당 ID가 없는 경우(잘못된 ID가 넘어온 경우) 예외 처리
            if (setting == null) {
                throw new EntityNotFoundException("존재하지 않는 셋팅입니다. ID: " + dto.getSettingId());
            }

            setting.updateIsActive(dto.getIsActive());
        });
    }
}
