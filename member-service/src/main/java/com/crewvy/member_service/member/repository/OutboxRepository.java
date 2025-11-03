package com.crewvy.member_service.member.repository;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.entity.NotificationOutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<NotificationOutboxEvent, UUID> {
    List<NotificationOutboxEvent> findAllByProcessed(Bool bool, Pageable pageable);
}
