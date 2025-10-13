package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DailyAttendanceRepository extends JpaRepository<DailyAttendance, UUID> {
    Optional<DailyAttendance> findByMemberIdAndAttendanceDate(UUID memberId, LocalDate attendanceDate);
}
