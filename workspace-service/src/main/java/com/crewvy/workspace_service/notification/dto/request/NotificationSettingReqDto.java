package com.crewvy.workspace_service.notification.dto.request;

import com.crewvy.common.entity.Bool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class NotificationSettingReqDto {
    private UUID settingId;
    private Bool isActive;
}
