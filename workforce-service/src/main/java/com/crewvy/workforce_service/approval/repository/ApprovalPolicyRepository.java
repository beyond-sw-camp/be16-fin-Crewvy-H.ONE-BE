package com.crewvy.workforce_service.approval.repository;

import com.crewvy.workforce_service.approval.entity.ApprovalPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalPolicyRepository extends JpaRepository<ApprovalPolicy, UUID> {

    List<ApprovalPolicy> findByApprovalDocument_Id(UUID documentId);

    List<ApprovalPolicy> findByApprovalDocument_IdAndCompanyId(UUID documentId, UUID companyId);
}
