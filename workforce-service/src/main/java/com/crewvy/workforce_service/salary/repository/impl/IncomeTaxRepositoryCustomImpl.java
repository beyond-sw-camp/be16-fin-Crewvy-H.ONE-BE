package com.crewvy.workforce_service.salary.repository.impl;

import com.crewvy.workforce_service.salary.repository.IncomeTaxRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.crewvy.workforce_service.salary.entity.QIncomeTax.incomeTax;

@Repository
@RequiredArgsConstructor
public class IncomeTaxRepositoryCustomImpl implements IncomeTaxRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Long findTaxAmount(long taxableIncome, int dependentCount) {

        return queryFactory
                .select(incomeTax.taxAmount)
                .from(incomeTax)
                .where(incomeTax.incomeStart.loe(taxableIncome),
                        incomeTax.incomeEnd.gt(taxableIncome),
                        incomeTax.dependentCount.eq(dependentCount)
                )
                .fetchOne();
    }
}
