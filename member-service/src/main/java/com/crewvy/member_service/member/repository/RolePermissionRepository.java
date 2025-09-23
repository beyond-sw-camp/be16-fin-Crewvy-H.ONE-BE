package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
}
