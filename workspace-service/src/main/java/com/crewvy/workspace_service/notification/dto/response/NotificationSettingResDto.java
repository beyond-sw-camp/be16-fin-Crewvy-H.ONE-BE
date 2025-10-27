package com.crewvy.workspace_service.notification.dto.response;

import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.notification.entity.NotificationSetting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class NotificationSettingResDto {
    private UUID settingId;
    private String type;
    private Bool isActive;

    public static NotificationSettingResDto from(NotificationSetting setting) {
        return NotificationSettingResDto.builder()
                .settingId(setting.getId())
                .type(setting.getNotificationType().getCodeName())
                .isActive(setting.getIsActive())
                .build();
    }
}
