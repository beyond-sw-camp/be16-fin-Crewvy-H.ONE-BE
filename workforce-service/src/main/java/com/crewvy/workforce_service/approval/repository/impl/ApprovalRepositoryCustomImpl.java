package com.crewvy.workforce_service.approval.repository.impl;


import com.crewvy.workforce_service.approval.constant.ApprovalState;
import com.crewvy.workforce_service.approval.entity.Approval;
import com.crewvy.workforce_service.approval.repository.ApprovalRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.crewvy.workforce_service.approval.entity.QApproval.approval;
import static com.crewvy.workforce_service.approval.entity.QApprovalDocument.approvalDocument;
import static com.crewvy.workforce_service.approval.entity.QApprovalLine.approvalLine;

@Repository
@RequiredArgsConstructor
public class ApprovalRepositoryCustomImpl implements ApprovalRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Approval> findByIdWithDetails(@Param("id") UUID id) {

        Approval result = queryFactory.selectFrom(approval)
                .join(approval.approvalDocument).fetchJoin()
                .leftJoin(approval.approvalLineList).fetchJoin()
                .where(approval.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<Approval> findByMemberPositionIdAndStateWithDocument(
            @Param("memberPositionId") UUID memberPositionId,
            @Param("state") ApprovalState state,
            Pageable pageable) {

        List<Approval> list = queryFactory
                .selectDistinct(approval)
                .join(approval.approvalDocument).fetchJoin()
                .where(approval.memberPositionId.eq(memberPositionId)
                        .and(approval.state.eq(state)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(approval.countDistinct())
                .from(approval)
                .where(approval.memberPositionId.eq(memberPositionId),
                        approval.state.eq(state))
                .fetchOne();

        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(list, pageable, totalCount);
    }

    @Override
    public Page<Approval> findByMemberPositionIdAndStateInWithDocument(
            @Param("memberPositionId") UUID memberPositionId,
            @Param("states") List<ApprovalState> states,
            Pageable pageable) {

        List<Approval> list = queryFactory
                .selectDistinct(approval)
                .join(approval.approvalDocument).fetchJoin()
                .where(approval.memberPositionId.eq(memberPositionId)
                        .and(approval.state.in(states)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(approval.countDistinct())
                .from(approval)
                .where(approval.memberPositionId.eq(memberPositionId),
                        approval.state.in(states))
                .fetchOne();

        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(list, pageable, totalCount);
    }

    @Override
    public Page<Approval> findByLineMemberPositionIdAndStateInWithDocument(
            @Param("lineMemberPositionId") UUID lineMemberPositionId,
            @Param("states") List<ApprovalState> states,
            Pageable pageable) {

        List<Approval> list = queryFactory
                .selectDistinct(approval)
                .join(approval.approvalDocument, approvalDocument).fetchJoin()
                .join(approval.approvalLineList, approvalLine)
                .where(approvalLine.memberPositionId.eq(lineMemberPositionId),
                        approvalLine.lineIndex.ne(1),
                        approval.state.in(states))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(approval.countDistinct())
                .from(approval)
                .join(approval.approvalLineList, approvalLine)
                .where(approvalLine.memberPositionId.eq(lineMemberPositionId),
                        approvalLine.lineIndex.ne(1),
                        approval.state.in(states))
                .fetchOne();

        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(list, pageable, totalCount);
    }

    @Override
    public int countByLineMemberPositionIdAndStateIn(@Param("lineMemberPositionId") UUID lineMemberPositionId,
                                                    @Param("states") List<ApprovalState> states) {

        Long count = queryFactory
                .select(approval.countDistinct())
                .from(approval)
                .join(approval.approvalLineList, approvalLine)
                .where(approvalLine.memberPositionId.eq(lineMemberPositionId),
                        approvalLine.lineIndex.ne(1),
                        approval.state.in(states))
                .fetchOne();

        return (count != null) ? count.intValue() : 0;
    }

    @Override
    public Optional<Approval> findByIdWithLines(@Param("id") UUID id) {

        Approval result = queryFactory
                .selectFrom(approval)
                .leftJoin(approval.approvalLineList, approvalLine).fetchJoin()
                .where(approval.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
