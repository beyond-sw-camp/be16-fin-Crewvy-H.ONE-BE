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

    List<PolicyAssignment> findByTargetIdAndScopeType(UUID targetId, PolicyScopeType scopeType);

    List<PolicyAssignment> findByTargetId(UUID targetId);

    @Query("SELECT pa FROM PolicyAssignment pa WHERE pa.targetId IN :targetIds AND pa.policy.companyId = :companyId AND pa.isActive = true AND pa.policy.effectiveFrom <= :currentDate AND (pa.policy.effectiveTo IS NULL OR pa.policy.effectiveTo >= :currentDate)")
    List<PolicyAssignment> findActiveAssignmentsByTargets(@Param("targetIds") List<UUID> targetIds, @Param("companyId") UUID companyId, @Param("currentDate") LocalDate currentDate);

    Page<PolicyAssignment> findAllByPolicy_CompanyId(UUID companyId, Pageable pageable);

    List<PolicyAssignment> findByTargetIdIn(List<UUID> targetIds);

    boolean existsByPolicy_IdAndIsActiveTrue(UUID policyId);

    @Query("SELECT pa FROM PolicyAssignment pa WHERE pa.policy.id = :policyId AND pa.targetId IN :targetIds AND pa.scopeType = :scopeType")
    List<PolicyAssignment> findByPolicyIdAndTargetIdInAndScopeType(@Param("policyId") UUID policyId, @Param("targetIds") List<UUID> targetIds, @Param("scopeType") PolicyScopeType scopeType);

    /**
     * Policy를 즉시 로딩하여 LazyInitializationException 방지
     */
    @Query("SELECT pa FROM PolicyAssignment pa JOIN FETCH pa.policy")
    List<PolicyAssignment> findAllWithPolicy();
}
