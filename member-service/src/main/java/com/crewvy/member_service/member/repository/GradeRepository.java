package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GradeRepository extends JpaRepository<Grade, UUID> {
    Optional<Grade> findByNameAndCompany(String s, Company company);
}
