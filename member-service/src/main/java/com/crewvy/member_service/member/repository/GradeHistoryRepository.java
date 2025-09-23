package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.GradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GradeHistoryRepository extends JpaRepository<GradeHistory, UUID> {
}
