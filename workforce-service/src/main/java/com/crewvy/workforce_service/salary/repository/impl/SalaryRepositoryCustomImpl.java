package com.crewvy.workforce_service.salary.repository.impl;

import com.crewvy.workforce_service.salary.constant.SalaryStatus;
import com.crewvy.workforce_service.salary.entity.Salary;
import com.crewvy.workforce_service.salary.repository.SalaryRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.crewvy.workforce_service.salary.entity.QSalary.salary;

@Repository
@RequiredArgsConstructor
public class SalaryRepositoryCustomImpl implements SalaryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsSalary(List<UUID> memberIds,
                                LocalDate startDate,
                                LocalDate endDate,
                                SalaryStatus status) {

        Integer fetchFirst = queryFactory
                .selectOne()
                .from(salary)
                .where(createCommonWhere(memberIds, startDate, endDate, status))
                .fetchFirst();

        return fetchFirst != null;
    }

    @Override
    public List<Salary> findAllActiveSalaries(List<UUID> memberIds,
                                              LocalDate startDate,
                                              LocalDate endDate,
                                              SalaryStatus status) {

        return queryFactory
                .selectFrom(salary)
                .where(createCommonWhere(memberIds, startDate, endDate, status))
                .fetch();
    }

    private BooleanBuilder createCommonWhere(List<UUID> memberIds,
                                             LocalDate startDate,
                                             LocalDate endDate,
                                             SalaryStatus status) {

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(salary.memberId.in(memberIds));
        builder.and(salary.paymentDate.between(startDate, endDate));
        builder.and(salary.salaryStatus.eq(status));

        return builder;
    }
}
