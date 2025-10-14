package com.crewvy.workforce_service.approval.repository;

import com.crewvy.workforce_service.approval.constant.LineStatus;
import com.crewvy.workforce_service.approval.entity.Approval;
import com.crewvy.workforce_service.approval.entity.ApprovalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalLineRepository extends JpaRepository<ApprovalLine, UUID> {
    // memberId와 lineStatus를 기준으로 ApprovalLine 목록을 찾는 메서드
    List<ApprovalLine> findByMemberPositionIdAndLineStatus(UUID memberId, LineStatus lineStatus);

    Optional<ApprovalLine> findByApprovalAndMemberPositionId(Approval approval, UUID memberId);

    Optional<ApprovalLine> findFirstByApprovalOrderByLineIndexDesc(Approval approval);
}
