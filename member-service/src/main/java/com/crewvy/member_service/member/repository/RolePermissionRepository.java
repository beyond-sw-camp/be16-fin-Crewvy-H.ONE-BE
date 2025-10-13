package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.Role;
import com.crewvy.member_service.member.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {
    List<RolePermission> findByRoleId(UUID uuid);
    void deleteAllByRole(Role role);
}
