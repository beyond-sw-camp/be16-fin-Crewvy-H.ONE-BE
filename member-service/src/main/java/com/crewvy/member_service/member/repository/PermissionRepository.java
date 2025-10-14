package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.constant.Action;
import com.crewvy.member_service.member.constant.PermissionRange;
import com.crewvy.member_service.member.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    Optional<Permission> findByResourceAndActionAndPermissionRange(String resource, Action action, PermissionRange permissionRange);

    @Query("SELECT COUNT(rp) > 0 " +
           "FROM RolePermission rp " +
           "WHERE rp.role = (SELECT mp.role FROM MemberPosition mp WHERE mp.id = :memberPositionId) " +
           "AND rp.permission.resource = :resource " +
           "AND rp.permission.action = :action " +
           "AND rp.permission.permissionRange = :permissionRange")
    boolean hasPermission(@Param("memberPositionId") UUID memberPositionId,
                          @Param("resource") String resource,
                          @Param("action") Action action,
                          @Param("permissionRange") PermissionRange permissionRange);

    List<Permission> findByPermissionRange(PermissionRange permissionRange);

    List<Permission> findAllByPermissionRangeNot(PermissionRange permissionRange);
}
