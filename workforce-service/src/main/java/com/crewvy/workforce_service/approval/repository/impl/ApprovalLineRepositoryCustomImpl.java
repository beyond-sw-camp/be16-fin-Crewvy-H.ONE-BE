package com.crewvy.workforce_service.approval.repository.impl;

import com.crewvy.workforce_service.approval.constant.LineStatus;
import com.crewvy.workforce_service.approval.entity.ApprovalLine;
import com.crewvy.workforce_service.approval.repository.ApprovalLineRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.crewvy.workforce_service.approval.entity.QApproval.approval;
import static com.crewvy.workforce_service.approval.entity.QApprovalDocument.approvalDocument;
import static com.crewvy.workforce_service.approval.entity.QApprovalLine.approvalLine;

@Repository
@RequiredArgsConstructor
public class ApprovalLineRepositoryCustomImpl implements ApprovalLineRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ApprovalLine> findPendingLinesWithDetails(
            @Param("memberPositionId") UUID memberPositionId,
            @Param("status") LineStatus status,
            Pageable pageable) {

        List<ApprovalLine> list =  queryFactory.selectFrom(approvalLine)
                .join(approvalLine.approval, approval).fetchJoin()
                .join(approval.approvalDocument, approvalDocument).fetchJoin()
                .where(approvalLine.memberPositionId.eq(memberPositionId)
                        .and(approvalLine.lineStatus.eq(status)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(approvalLine.count())
                .from(approvalLine)
                .where(
                        approvalLine.memberPositionId.eq(memberPositionId),
                        approvalLine.lineStatus.eq(status)
                )
                .fetchOne();

        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(list, pageable, totalCount);

    }

}
