package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.entity.FixedAllowance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FixedAllowanceRepository extends JpaRepository<FixedAllowance, Long> {
    List<FixedAllowance> findAllByCompanyId(UUID companyId);

    Optional<FixedAllowance> findByMemberIdAndAllowanceName(UUID memberId, String allowanceName);
}
