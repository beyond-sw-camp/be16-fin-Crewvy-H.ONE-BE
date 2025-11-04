package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.entity.PayrollItem;

import java.util.List;
import java.util.UUID;

public interface PayrollItemRepositoryCustom {
    List<PayrollItem> findApplicableForCompany(UUID companyId, SalaryType salaryType);
}
