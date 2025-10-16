package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    List<Organization> findByCompanyOrderByDisplayOrderAsc(Company company);
    List<Organization> findByParentIdOrderByDisplayOrderAsc(UUID parentId);

    List<Organization> findAllByCompany(Company company);
}