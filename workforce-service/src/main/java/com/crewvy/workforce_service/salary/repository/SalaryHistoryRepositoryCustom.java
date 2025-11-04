package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.entity.SalaryHistory;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SalaryHistoryRepositoryCustom {
    List<SalaryHistory> findLatestSalaryHistoriesByCompany(UUID companyId, LocalDate targetDate);
}
