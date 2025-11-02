package com.crewvy.workforce_service.approval.repository;

import com.crewvy.workforce_service.approval.entity.SearchOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SearchOutboxEventRepository extends JpaRepository<SearchOutboxEvent, UUID> {
}
