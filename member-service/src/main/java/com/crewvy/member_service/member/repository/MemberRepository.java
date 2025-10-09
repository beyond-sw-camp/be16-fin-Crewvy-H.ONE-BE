package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.defaultMemberPosition WHERE m.email = :email")
    Optional<Member> findByEmail(@Param("email") String email);

    @Query("SELECT m FROM Member m " +
           "LEFT JOIN FETCH m.defaultMemberPosition mp " +
           "LEFT JOIN FETCH mp.title " +
           "LEFT JOIN FETCH mp.organization " +
           "WHERE m.company = :company")
    List<Member> findByCompanyWithDetail(@Param("company") Company company);

    @Query("SELECT m FROM Member m " +
           "LEFT JOIN FETCH m.memberPositionList mp " +
           "LEFT JOIN FETCH mp.organization " +
           "LEFT JOIN FETCH mp.title " +
           "LEFT JOIN FETCH mp.role " +
           "LEFT JOIN FETCH m.gradeHistorySet " +
           "WHERE m.id = :id")
    Optional<Member> findByIdWithDetail(@Param("id") UUID id);
}
