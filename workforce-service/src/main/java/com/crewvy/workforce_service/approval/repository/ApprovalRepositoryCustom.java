package com.crewvy.workforce_service.approval.repository;

import com.crewvy.workforce_service.approval.constant.ApprovalState;
import com.crewvy.workforce_service.approval.entity.Approval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalRepositoryCustom {
    Optional<Approval> findByIdWithDetails(@Param("id") UUID id);

    Page<Approval> findByMemberPositionIdAndStateWithDocument(
            @Param("memberPositionId") UUID memberPositionId,
            @Param("state") ApprovalState state,
            Pageable pageable);

    Page<Approval> findByMemberPositionIdAndStateInWithDocument(
            @Param("memberPositionId") UUID memberPositionId,
            @Param("states") List<ApprovalState> states,
            Pageable pageable);

    Page<Approval> findByLineMemberPositionIdAndStateInWithDocument(
            @Param("lineMemberPositionId") UUID lineMemberPositionId,
            @Param("states") List<ApprovalState> states,
            Pageable pageable
    );

    int countByLineMemberPositionIdAndStateIn(@Param("lineMemberPositionId") UUID lineMemberPositionId,
                                              @Param("states") List<ApprovalState> states);

    Optional<Approval> findByIdWithLines(@Param("id") UUID id);
}
