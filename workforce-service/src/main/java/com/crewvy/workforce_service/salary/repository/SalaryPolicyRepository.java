package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.entity.SalaryPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalaryPolicyRepository extends JpaRepository<SalaryPolicy, UUID> {
    Optional<SalaryPolicy> findByCompanyId(UUID companyId);
    boolean existsByCompanyId(UUID companyId);
}
