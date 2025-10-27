package com.crewvy.workspace_service.notification.dto.response;

import com.crewvy.workspace_service.notification.constant.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class NotificationResDto {
    private UUID notificationId;
    private String type;
    private String contents;
    private UUID targetId;
    private LocalDateTime createAt;
}
