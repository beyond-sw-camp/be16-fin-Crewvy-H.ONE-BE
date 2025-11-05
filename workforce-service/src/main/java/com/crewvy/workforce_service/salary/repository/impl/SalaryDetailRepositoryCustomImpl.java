package com.crewvy.workforce_service.salary.repository.impl;

import com.crewvy.workforce_service.salary.dto.response.ItemTotalRes;
import com.crewvy.workforce_service.salary.repository.SalaryDetailRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.crewvy.workforce_service.salary.entity.QSalaryDetail.salaryDetail;

@Repository
@RequiredArgsConstructor
public class SalaryDetailRepositoryCustomImpl implements SalaryDetailRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ItemTotalRes> sumAmountsBySalaryName(List<UUID> salaryIdList) {

        return queryFactory
                .select(Projections.constructor(ItemTotalRes.class,
                        salaryDetail.salaryName, salaryDetail.amount.sum()))
                .from(salaryDetail)
                .where(salaryDetail.salary.id.in(salaryIdList))
                .groupBy(salaryDetail.salaryName)
                .fetch();
    }
}
