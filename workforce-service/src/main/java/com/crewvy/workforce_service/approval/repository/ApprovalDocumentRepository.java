package com.crewvy.workforce_service.approval.repository;

import com.crewvy.workforce_service.approval.entity.ApprovalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApprovalDocumentRepository extends JpaRepository<ApprovalDocument, UUID> {

}
