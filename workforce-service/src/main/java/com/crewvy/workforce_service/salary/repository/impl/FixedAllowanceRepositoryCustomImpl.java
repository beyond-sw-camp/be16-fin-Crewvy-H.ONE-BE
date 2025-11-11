package com.crewvy.workforce_service.salary.repository.impl;

import com.crewvy.workforce_service.salary.entity.FixedAllowance;
import com.crewvy.workforce_service.salary.entity.QFixedAllowance;
import com.crewvy.workforce_service.salary.repository.FixedAllowanceRepositoryCustom;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.crewvy.workforce_service.salary.entity.QFixedAllowance.fixedAllowance;

@Repository
@RequiredArgsConstructor
public class FixedAllowanceRepositoryCustomImpl implements FixedAllowanceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<FixedAllowance> findActiveAllowances(UUID companyId, LocalDate referenceDate) {

        QFixedAllowance sh2 = new QFixedAllowance("sh2");

        return queryFactory
                .selectFrom(fixedAllowance)
                .where(
                        fixedAllowance.companyId.eq(companyId),
                        fixedAllowance.effectiveDate.loe(referenceDate),
                        fixedAllowance.effectiveDate.eq(
                                JPAExpressions
                                        .select(sh2.effectiveDate.max())
                                        .from(sh2)
                                        .where(sh2.memberId.eq(fixedAllowance.memberId),
                                                sh2.allowanceName.eq(fixedAllowance.allowanceName),
                                                sh2.effectiveDate.loe(referenceDate)
                                        )
                        )
                )
                .fetch();
    }
}
