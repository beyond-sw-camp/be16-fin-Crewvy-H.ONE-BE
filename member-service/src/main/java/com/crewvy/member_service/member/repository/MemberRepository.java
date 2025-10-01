package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.defaultMemberPosition WHERE m.email = :email")
    Optional<Member> findByEmail(@Param("email") String email);
}
