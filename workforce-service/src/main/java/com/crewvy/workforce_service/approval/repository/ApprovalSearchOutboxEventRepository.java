package com.crewvy.workforce_service.approval.repository;

import com.crewvy.common.entity.Bool;
import com.crewvy.workforce_service.approval.entity.ApprovalSearchOutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalSearchOutboxEventRepository extends JpaRepository<ApprovalSearchOutboxEvent, UUID> {
    List<ApprovalSearchOutboxEvent> findAllByProcessed(Bool bool, Pageable pageable);
}
