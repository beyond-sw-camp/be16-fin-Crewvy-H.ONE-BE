package com.crewvy.member_service.member.repository;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.entity.GradeHistory;
import com.crewvy.member_service.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GradeHistoryRepository extends JpaRepository<GradeHistory, UUID> {
    Optional<GradeHistory> findByMemberAndIsActive(Member member, Bool isActive);
}
