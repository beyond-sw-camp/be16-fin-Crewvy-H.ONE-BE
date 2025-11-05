package com.crewvy.workforce_service.approval.repository;

import com.crewvy.common.entity.Bool;
import com.crewvy.workforce_service.approval.entity.ApprovalDocument;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalDocumentRepository extends JpaRepository<ApprovalDocument, UUID> {
    @Query("SELECT d FROM ApprovalDocument d LEFT JOIN FETCH d.policyList WHERE d.id = :id")
    Optional<ApprovalDocument> findByIdWithPolicies(@Param("id") UUID id);

    Optional<ApprovalDocument> findByDocumentName(String documentName);

    List<ApprovalDocument> findByIsDirectCreatable(Bool bool);
}
