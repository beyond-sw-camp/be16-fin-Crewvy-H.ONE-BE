package com.crewvy.workforce_service.salary.repository.impl;

import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import com.crewvy.workforce_service.salary.repository.PayrollItemRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.crewvy.workforce_service.salary.entity.QPayrollItem.payrollItem;

@Repository
@RequiredArgsConstructor
public class PayrollItemRepositoryImpl implements PayrollItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<PayrollItem> findApplicableForCompany(
            UUID companyId, SalaryType salaryType) {

        return queryFactory
                .selectFrom(payrollItem)
                .where(payrollItem.salaryType.eq(salaryType),
                        (payrollItem.companyId.isNull().and(payrollItem.calculationCode.isNotNull()))
                                .or(payrollItem.companyId.eq(companyId).and(payrollItem.calculationCode.isNull()))
                )
                .fetch();
    }
}
