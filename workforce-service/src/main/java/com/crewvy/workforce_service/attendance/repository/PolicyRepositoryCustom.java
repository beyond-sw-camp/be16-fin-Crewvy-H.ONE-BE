package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface PolicyRepositoryCustom {

    Page<Policy> findActivePolicies(@Param("companyId") UUID companyId,
                                    @Param("currentDate") LocalDate currentDate, Pageable pageable);
}
