package com.crewvy.workforce_service.approval.repository;

import com.crewvy.workforce_service.approval.constant.ApprovalState;
import com.crewvy.workforce_service.approval.entity.Approval;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, UUID> {
    List<Approval> findByMemberPositionIdAndState(UUID memberId, ApprovalState state);
    List<Approval> findByMemberPositionIdAndStateIn(UUID memberId, List<ApprovalState> stateList);

    List<Approval> findByState(ApprovalState approvalState);

    List<Approval> findByStateIn(List<ApprovalState> stateList);

    int countByState(ApprovalState approvalState);

    int countByMemberPositionIdAndState(UUID memberId, ApprovalState approvalState);

    int countByMemberPositionIdAndStateIn(UUID memberId, List<ApprovalState> stateList);

    int countByStateIn(List<ApprovalState> stateList);

    @Query("SELECT a FROM Approval a " +
            "JOIN FETCH a.approvalDocument " +
            "LEFT JOIN FETCH a.approvalLineList " + // lineList는 여전히 한 번에 가져옵니다.
            "WHERE a.id = :id") // attachmentList에 대한 JOIN FETCH를 제거했습니다.
    Optional<Approval> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT a FROM Approval a JOIN FETCH a.approvalDocument WHERE a.memberPositionId = :memberPositionId AND a.state = :state")
    List<Approval> findByMemberPositionIdAndStateWithDocument(@Param("memberPositionId") UUID memberPositionId, @Param("state") ApprovalState state);

    @Query("SELECT a FROM Approval a JOIN FETCH a.approvalDocument WHERE a.memberPositionId = :memberPositionId AND a.state IN :states")
    List<Approval> findByMemberPositionIdAndStateInWithDocument(
            @Param("memberPositionId") UUID memberPositionId,
            @Param("states") List<ApprovalState> states
    );

    @Query("SELECT a FROM Approval a LEFT JOIN FETCH a.approvalLineList WHERE a.id = :id")
    Optional<Approval> findByIdWithLines(@Param("id") UUID id);
}
