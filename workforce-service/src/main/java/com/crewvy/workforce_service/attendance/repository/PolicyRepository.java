package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.entity.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {
    @Query("SELECT p FROM Policy p WHERE p.companyId = :companyId " +
           "AND p.effectiveFrom <= :currentDate " +
           "AND (p.effectiveTo IS NULL OR p.effectiveTo >= :currentDate)")
    Page<Policy> findActivePolicies(@Param("companyId") UUID companyId, @Param("currentDate") LocalDate currentDate, Pageable pageable);
    Page<Policy> findByCompanyId(UUID companyId, Pageable pageable);

    @Query("SELECT DISTINCT p.companyId FROM Policy p")
    List<UUID> findDistinctCompanyIds();

    /**
     * 회사 + 정책 타입으로 정책 조회 (연차 정책 조회용)
     * 연차 정책은 회사 레벨에만 할당되므로 직접 조회 가능
     *
     * @param companyId 회사 ID
     * @param typeCode 정책 타입 코드
     * @return 해당 정책 (Optional)
     */
    @Query("SELECT p FROM Policy p " +
           "WHERE p.companyId = :companyId " +
           "AND p.policyTypeCode = :typeCode " +
           "AND p.isActive = true")
    Optional<Policy> findByCompanyIdAndPolicyTypeCode(
            @Param("companyId") UUID companyId,
            @Param("typeCode") PolicyTypeCode typeCode
    );

    /**
     * 활성 연차유급휴가 정책 조회
     *
     * @param companyId 회사 ID
     * @return 연차 정책 (Optional)
     */
    @Query("SELECT p FROM Policy p " +
           "WHERE p.companyId = :companyId " +
           "AND p.policyTypeCode = com.crewvy.workforce_service.attendance.constant.PolicyTypeCode.ANNUAL_LEAVE " +
           "AND p.isActive = true " +
           "AND p.effectiveFrom <= CURRENT_DATE " +
           "AND (p.effectiveTo IS NULL OR p.effectiveTo >= CURRENT_DATE)")
    Optional<Policy> findActiveAnnualLeavePolicy(@Param("companyId") UUID companyId);
}
