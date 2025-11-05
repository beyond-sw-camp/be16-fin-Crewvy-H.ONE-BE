package com.crewvy.workforce_service.approval.repository;

import com.crewvy.workforce_service.approval.entity.ApprovalDocument;

import java.util.Optional;
import java.util.UUID;

public interface ApprovalDocumentRepositoryCustom {
    Optional<ApprovalDocument> findByIdWithPolicies(UUID id);
}
