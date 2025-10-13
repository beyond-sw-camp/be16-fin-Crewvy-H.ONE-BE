package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.entity.SalaryInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalaryInfoRepository extends JpaRepository<SalaryInfo, UUID> {

    List<SalaryInfo> findByCompanyIdOrderByCreatedAtAsc(UUID companyId);
}
