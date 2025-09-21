package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
}
