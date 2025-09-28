package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.MemberPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemberOrgTitleRepository extends JpaRepository<MemberPosition, UUID> {
}
