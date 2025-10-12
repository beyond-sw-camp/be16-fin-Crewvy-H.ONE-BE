package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.MemberPosition;
import com.crewvy.member_service.member.entity.Organization;
import com.crewvy.member_service.member.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemberPositionRepository extends JpaRepository<MemberPosition, UUID> {
    List<MemberPosition> findAllByOrganization(Organization organization);
    List<MemberPosition> findByRole(Role role);
}