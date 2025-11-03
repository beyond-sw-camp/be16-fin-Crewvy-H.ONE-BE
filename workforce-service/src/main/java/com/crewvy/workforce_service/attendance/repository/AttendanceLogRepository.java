package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.constant.EventType;
import com.crewvy.workforce_service.attendance.entity.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, UUID> {
    Optional<AttendanceLog> findTopByMemberIdAndEventTypeOrderByEventTimeDesc(UUID memberId, EventType eventType);

    List<AttendanceLog> findByMemberIdAndEventTimeBetweenOrderByEventTimeDesc(UUID memberId, LocalDateTime startTime, LocalDateTime endTime);
}
