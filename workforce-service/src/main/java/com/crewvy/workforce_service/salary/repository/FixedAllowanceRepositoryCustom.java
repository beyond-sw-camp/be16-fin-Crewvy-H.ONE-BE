package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.entity.FixedAllowance;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FixedAllowanceRepositoryCustom {
    List<FixedAllowance> findActiveAllowances(UUID companyId, LocalDate endDate);
}
