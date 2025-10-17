package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.constant.PolicyScopeType;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.PolicyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PolicyAssignmentRepository extends JpaRepository<PolicyAssignment, UUID> {
    boolean existsByPolicyAndTargetIdAndTargetType(Policy policy, UUID targetId, PolicyScopeType targetType);

    Optional<PolicyAssignment> findFirstByTargetIdAndTargetTypeAndIsActiveTrueOrderByAssignedAtDesc(UUID targetId, PolicyScopeType targetType);

    Optional<PolicyAssignment> findFirstByTargetIdInAndTargetTypeAndIsActiveTrueOrderByAssignedAtDesc(List<UUID> targetIds, PolicyScopeType targetType);
}
