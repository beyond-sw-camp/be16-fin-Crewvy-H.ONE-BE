package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {
    @Query("SELECT p FROM Policy p WHERE p.companyId = :companyId " +
           "AND p.effectiveFrom <= :currentDate " +
           "AND (p.effectiveTo IS NULL OR p.effectiveTo >= :currentDate)")
    Page<Policy> findActivePolicies(@Param("companyId") UUID companyId, @Param("currentDate") LocalDate currentDate, Pageable pageable);
    Page<Policy> findByCompanyId(UUID companyId, Pageable pageable);
}
