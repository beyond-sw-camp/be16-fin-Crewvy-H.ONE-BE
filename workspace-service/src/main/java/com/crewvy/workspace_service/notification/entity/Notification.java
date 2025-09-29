package com.crewvy.workspace_service.notification.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.notification.constant.NotificationType;
import com.crewvy.workspace_service.notification.converter.NotificationTypeConverter;
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
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "notification_type", nullable = false)
    @Convert(converter = NotificationTypeConverter.class)
    private NotificationType notificationType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "is_deleted", nullable = false)
    private Bool isDeleted = Bool.FALSE;
}
