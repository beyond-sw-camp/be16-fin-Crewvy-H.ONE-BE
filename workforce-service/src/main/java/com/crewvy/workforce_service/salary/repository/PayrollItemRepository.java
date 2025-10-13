package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollItemRepository extends JpaRepository<PayrollItem, UUID> {

    List<PayrollItem> findByCompanyIdOrderByCreatedAtAsc(UUID companyId);

    List<PayrollItem> findByCompanyIdAndSalaryTypeOrderByCreatedAtAsc(UUID companyId, SalaryType salaryType);
}
