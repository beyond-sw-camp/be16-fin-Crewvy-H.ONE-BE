package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.constant.PolicyScopeType;
import com.crewvy.workforce_service.attendance.entity.PolicyAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PolicyAssignmentRepository extends JpaRepository<PolicyAssignment, UUID> {
    @Query("SELECT pa FROM PolicyAssignment pa JOIN FETCH pa.policy p " +
           "WHERE pa.targetId IN :targetIds " +
           "AND p.isActive = true " +
           "AND p.effectiveFrom <= :date AND (p.effectiveTo IS NULL OR p.effectiveTo >= :date)")
    List<PolicyAssignment> findActiveAssignmentsByTargets(@Param("targetIds") List<UUID> targetIds, @Param("date") LocalDate date);

    Page<PolicyAssignment> findAllByCompanyId(UUID companyId, Pageable pageable);

    List<PolicyAssignment> findByTargetId(UUID targetId);

    List<PolicyAssignment> findByTargetIdAndScopeType(UUID targetId, PolicyScopeType scopeType);

    List<PolicyAssignment> findByTargetIdIn(List<UUID> targetIds);

    boolean existsByPolicy_IdAndIsActiveTrue(UUID policyId);
}
