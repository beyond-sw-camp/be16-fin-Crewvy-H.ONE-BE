package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.SearchOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SearchOutboxEventRepository extends JpaRepository<SearchOutboxEvent, UUID> {
}
