package com.crewvy.workspace_service.notification.repository;

import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByReceiverIdAndIsDeleted(UUID memberId, Bool bool);
}
