package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, UUID> {
}
