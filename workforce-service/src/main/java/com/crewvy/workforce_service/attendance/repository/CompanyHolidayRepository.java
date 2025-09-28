package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.CompanyHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyHolidayRepository extends JpaRepository<CompanyHoliday, UUID> {
}
