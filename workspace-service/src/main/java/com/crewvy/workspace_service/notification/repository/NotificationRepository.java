package com.crewvy.workspace_service.notification.repository;

import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.notification.entity.Notification;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByReceiverIdAndIsDeleted(UUID memberId, Bool bool);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Notification n " +
            "SET n.isDeleted = :newStatus " + // <-- (수정) 파라미터로 변경
            "WHERE n.receiverId = :memberId " +
            "AND n.isDeleted = :oldStatus") // <-- (수정) 파라미터로 변경
    int readAllByReceiverId(
            @Param("memberId") UUID memberId,
            @Param("newStatus") Bool newStatus, // (추가) 새 상태 파라미터
            @Param("oldStatus") Bool oldStatus  // (추가) 기존 상태 파라미터
    );
}
