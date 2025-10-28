package com.crewvy.workforce_service.salary.repository;

import com.crewvy.common.entity.Bool;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollItemRepository extends JpaRepository<PayrollItem, UUID> {

    List<PayrollItem> findByCompanyIdOrCompanyIdIsNull(UUID companyId);

    List<PayrollItem> findByCompanyIdAndSalaryTypeOrderByCreatedAtAsc(UUID companyId, SalaryType salaryType);

    List<PayrollItem> findByCompanyIdAndSalaryTypeAndIsTaxable(UUID companyId, SalaryType salaryType, Bool isTaxable);

    long countByCalculationCodeIsNotNull();
    
    List<PayrollItem> findByCompanyIdIsNullAndSalaryTypeAndIsTaxableAndIsActive(SalaryType salaryType
                                                                                , Bool isTaxable
                                                                                , Bool isActive);

    // 수당 계산 항목 조회
    List<PayrollItem> findByCompanyIdIsNullAndSalaryTypeAndIsTaxableAndCalculationCodeNot(
            SalaryType salaryType, // SalaryType.ST001
            Bool isTaxable,      // true
            String calculationCode   // "BASE_SALARY"
    );
}
