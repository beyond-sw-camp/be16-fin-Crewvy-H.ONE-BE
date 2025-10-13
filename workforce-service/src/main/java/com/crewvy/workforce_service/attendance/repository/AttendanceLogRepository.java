package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.constant.EventType;
import com.crewvy.workforce_service.attendance.entity.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, UUID> {
    Optional<AttendanceLog> findTopByMemberIdAndEventTypeOrderByEventTimeDesc(UUID memberId, EventType eventType);
}
