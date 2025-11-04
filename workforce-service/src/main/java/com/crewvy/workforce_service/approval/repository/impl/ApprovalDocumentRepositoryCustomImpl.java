package com.crewvy.workforce_service.approval.repository.impl;

import com.crewvy.workforce_service.approval.entity.ApprovalDocument;
import com.crewvy.workforce_service.approval.repository.ApprovalDocumentRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.crewvy.workforce_service.approval.entity.QApprovalDocument.approvalDocument;

@Repository
@RequiredArgsConstructor
public class ApprovalDocumentRepositoryCustomImpl implements ApprovalDocumentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<ApprovalDocument> findByIdWithPolicies(UUID id) {

        List<ApprovalDocument> results = queryFactory
                .selectFrom(approvalDocument).distinct()
                .leftJoin(approvalDocument.policyList).fetchJoin()
                .where(approvalDocument.id.eq(id))
                .fetch();

        ApprovalDocument result = results.isEmpty() ? null : results.get(0);

        return Optional.ofNullable(result);
    }
}
