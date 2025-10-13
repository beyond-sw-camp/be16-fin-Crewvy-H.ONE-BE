package com.crewvy.workforce_service.approval.repository;

import com.crewvy.workforce_service.approval.entity.ApprovalReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalReplyRepository extends JpaRepository<ApprovalReply, UUID> {
    List<ApprovalReply> findByApprovalId(UUID approvalId);
}
