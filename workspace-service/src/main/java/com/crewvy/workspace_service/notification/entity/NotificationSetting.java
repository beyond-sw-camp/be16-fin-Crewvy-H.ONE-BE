package com.crewvy.workspace_service.notification.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.notification.constant.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSetting extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "is_active", nullable = false)
    private Bool isActive = Bool.TRUE;

    public void updateIsActive(Bool bool) {
        this.isActive = bool;
    }
}
