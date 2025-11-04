package com.crewvy.workforce_service.attendance.repository.impl;

import com.crewvy.workforce_service.attendance.entity.PolicyAssignment;
import com.crewvy.workforce_service.attendance.repository.PolicyAssignmentRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.crewvy.workforce_service.attendance.entity.QPolicyAssignment.policyAssignment;

@Repository
@RequiredArgsConstructor
public class PolicyAssignmentRepositoryCustomImpl implements PolicyAssignmentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<PolicyAssignment> findActiveAssignmentsByTargets(@Param("targetIds") List<UUID> targetIds,
                                                                 @Param("companyId") UUID companyId,
                                                                 @Param("currentDate") LocalDate currentDate) {

        return queryFactory
                .selectFrom(policyAssignment) // "SELECT pa FROM PolicyAssignment pa"
                .where(policyAssignment.targetId.in(targetIds),
                        policyAssignment.policy.companyId.eq(companyId),
                        policyAssignment.isActive.isTrue(),
                        policyAssignment.policy.effectiveFrom.loe(currentDate),
                        (policyAssignment.policy.effectiveTo.isNull()
                                        .or(policyAssignment.policy.effectiveTo.goe(currentDate)))
                )
                .fetch();
    }
}
