package com.crewvy.workforce_service.salary.repository;

import com.crewvy.common.entity.Bool;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollItemRepository extends JpaRepository<PayrollItem, UUID>, PayrollItemRepositoryCustom {

    List<PayrollItem> findByCompanyIdOrCompanyIdIsNull(UUID companyId);

    List<PayrollItem> findByCompanyIdAndSalaryType(UUID companyId, SalaryType salaryType);

    long countByCalculationCodeIsNotNull();

    // 수당 계산 항목 조회
    List<PayrollItem> findByCompanyIdIsNullAndSalaryTypeAndIsTaxableAndCalculationCodeNot(
            SalaryType salaryType,
            Bool isTaxable,
            String calculationCode
    );

    // 고정 지급 항목 조회
    List<PayrollItem> findByCompanyIdAndSalaryTypeAndCalculationCodeIsNullAndIsTaxable(
            UUID companyId,
            SalaryType salaryType,
            Bool isTaxable
    );
}
