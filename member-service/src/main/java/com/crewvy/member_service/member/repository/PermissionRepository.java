package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
}
