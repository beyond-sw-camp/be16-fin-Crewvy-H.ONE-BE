package com.crewvy.workspace_service.meeting.repository;

import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.meeting.entity.MinuteSearchOutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MinuteSearchOutboxEventRepository extends JpaRepository<MinuteSearchOutboxEvent, UUID> {
    List<MinuteSearchOutboxEvent> findAllByProcessed(Bool bool, Pageable pageable);
}
