package com.crewvy.workforce_service.approval.repository;

import com.crewvy.workforce_service.approval.constant.ApprovalState;
import com.crewvy.workforce_service.approval.entity.Approval;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, UUID>, ApprovalRepositoryCustom {
    int countByMemberPositionIdAndState(UUID memberId, ApprovalState approvalState);

    int countByMemberPositionIdAndStateIn(UUID memberId, List<ApprovalState> stateList);

    @Query("SELECT a FROM Approval a " +
            "JOIN FETCH a.approvalDocument " +
            "LEFT JOIN FETCH a.approvalLineList " + // lineList는 여전히 한 번에 가져옵니다.
            "WHERE a.id = :id") // attachmentList에 대한 JOIN FETCH를 제거했습니다.
    Optional<Approval> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT DISTINCT a FROM Approval a " +
            "JOIN FETCH a.approvalDocument ad " +
            "WHERE a.memberPositionId = :memberPositionId " +
            "AND a.state = :state")
    Page<Approval> findByMemberPositionIdAndStateWithDocument(
            @Param("memberPositionId") UUID memberPositionId,
            @Param("state") ApprovalState state,
            Pageable pageable // pageable 추가
    );

    @Query("SELECT DISTINCT a FROM Approval a " +
            "JOIN FETCH a.approvalDocument ad " +
            "WHERE a.memberPositionId = :memberPositionId " +
            "AND a.state IN :states") // 여러 상태를 조회하므로 IN 사용
    Page<Approval> findByMemberPositionIdAndStateInWithDocument(
            @Param("memberPositionId") UUID memberPositionId,
            @Param("states") List<ApprovalState> states, // List<ApprovalState> 받도록
            Pageable pageable // pageable 추가
    );

    @Query("SELECT DISTINCT a FROM Approval a " +
            "JOIN FETCH a.approvalDocument ad " +
            "JOIN a.approvalLineList al " +         // 일반 JOIN
            "WHERE al.memberPositionId = :lineMemberPositionId " + // 내가 결재 라인에 있고
            "AND al.lineIndex != 1 " + // (추가!) 내가 기안자가 아니고 (lineIndex가 1이 아님)
            "AND a.state IN :states")
    Page<Approval> findByLineMemberPositionIdAndStateInWithDocument(
            @Param("lineMemberPositionId") UUID lineMemberPositionId,
            @Param("states") List<ApprovalState> states,
            Pageable pageable // pageable 추가
    );

    @Query("SELECT count(DISTINCT a) FROM Approval a " +
            "JOIN a.approvalLineList al " +
            "WHERE al.memberPositionId = :lineMemberPositionId " +
            "AND al.lineIndex != 1 " +
            "AND a.state IN :states")
    int countByLineMemberPositionIdAndStateIn( // 반환 타입을 int로 변경
                                               @Param("lineMemberPositionId") UUID lineMemberPositionId,
                                               @Param("states") List<ApprovalState> states
    );

    @Query("SELECT a FROM Approval a LEFT JOIN FETCH a.approvalLineList WHERE a.id = :id")
    Optional<Approval> findByIdWithLines(@Param("id") UUID id);
}
