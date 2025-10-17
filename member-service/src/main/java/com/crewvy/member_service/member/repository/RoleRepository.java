package com.crewvy.member_service.member.repository;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByNameAndCompany(String name, Company company);
    List<Role> findAllByCompanyAndYnDel(Company company, Bool ynDel);
    List<Role> findAllByCompanyAndYnDelOrderByDisplayOrderAsc(Company company, Bool ynDel); // displayOrder로 정렬 추가
}
