package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyAttendanceRepositoryCustom {

    List<DailyAttendance> findAllByDateRange(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    List<DailyAttendance> findAllByDateRangeAndCompany(
            @Param("companyId") UUID companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
