package com.crewvy.workforce_service.approval.repository;

import com.crewvy.workforce_service.approval.entity.ApprovalSearchOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SearchOutboxEventRepository extends JpaRepository<ApprovalSearchOutboxEvent, UUID> {
}
