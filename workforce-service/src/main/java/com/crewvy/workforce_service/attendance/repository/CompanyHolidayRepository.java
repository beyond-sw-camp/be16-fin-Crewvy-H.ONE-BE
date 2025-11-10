package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.CompanyHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface CompanyHolidayRepository extends JpaRepository<CompanyHoliday, UUID> {

    /**
     * 특정 날짜가 회사 휴일인지 확인
     */
    boolean existsByCompanyIdAndHolidayDate(UUID companyId, LocalDate holidayDate);
}
