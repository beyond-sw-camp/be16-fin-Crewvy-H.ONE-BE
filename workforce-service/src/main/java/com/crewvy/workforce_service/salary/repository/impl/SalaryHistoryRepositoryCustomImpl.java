package com.crewvy.workforce_service.salary.repository.impl;

import com.crewvy.workforce_service.salary.entity.QSalaryHistory;
import com.crewvy.workforce_service.salary.entity.SalaryHistory;
import com.crewvy.workforce_service.salary.repository.SalaryHistoryRepositoryCustom;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.crewvy.workforce_service.salary.entity.QSalaryHistory.salaryHistory;

@Repository
@RequiredArgsConstructor
public class SalaryHistoryRepositoryCustomImpl implements SalaryHistoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<SalaryHistory> findLatestSalaryHistoriesByCompany(
            UUID companyId, LocalDate targetDate) {

        QSalaryHistory sh2 = new QSalaryHistory("sh2");

        return queryFactory
                .selectFrom(salaryHistory)
                .where(salaryHistory.companyId.eq(companyId),
                        salaryHistory.effectiveDate.loe(targetDate),
                        salaryHistory.effectiveDate.eq(JPAExpressions
                                        .select(sh2.effectiveDate.max())
                                        .from(sh2)
                                        .where(sh2.memberId.eq(salaryHistory.memberId),
                                                sh2.effectiveDate.loe(targetDate)
                                        )
                        )
                )
                .fetch();
    }
}

