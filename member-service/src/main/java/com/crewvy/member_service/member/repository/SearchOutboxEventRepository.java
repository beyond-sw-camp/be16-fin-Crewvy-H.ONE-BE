package com.crewvy.member_service.member.repository;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.entity.SearchOutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SearchOutboxEventRepository extends JpaRepository<SearchOutboxEvent, UUID> {
    List<SearchOutboxEvent> findAllByProcessed(Bool bool, Pageable pageable);
}
