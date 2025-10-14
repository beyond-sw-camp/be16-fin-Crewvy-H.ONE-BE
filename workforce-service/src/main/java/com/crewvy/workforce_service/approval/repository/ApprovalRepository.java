package com.crewvy.workforce_service.approval.repository;

import com.crewvy.workforce_service.approval.constant.ApprovalState;
import com.crewvy.workforce_service.approval.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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
}
