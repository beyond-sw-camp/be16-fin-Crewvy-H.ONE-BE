package com.crewvy.workforce_service.attendance.repository.impl;

import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.repository.PolicyRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.crewvy.workforce_service.approval.entity.QApprovalLine.approvalLine;
import static com.crewvy.workforce_service.attendance.entity.QPolicy.policy;

@Repository
@RequiredArgsConstructor
public class PolicyRepositoryCustomImpl  implements PolicyRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Policy> findActivePolicies(@Param("companyId") UUID companyId,
                                    @Param("currentDate") LocalDate currentDate, Pageable pageable) {

        List<Policy> list = queryFactory
                .selectFrom(policy)
                .where(policy.companyId.eq(companyId),
                        policy.effectiveFrom.loe(currentDate),
                        (policy.effectiveTo.isNull()
                                .or(policy.effectiveTo.goe(currentDate))))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(policy.count())
                .from(policy)
                .where(policy.companyId.eq(companyId),
                        policy.effectiveFrom.loe(currentDate),
                        (policy.effectiveTo.isNull()
                                .or(policy.effectiveTo.goe(currentDate))))
                .fetchOne();

        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(list, pageable, totalCount);

    }
}
