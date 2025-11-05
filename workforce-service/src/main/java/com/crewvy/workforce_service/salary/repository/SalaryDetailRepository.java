package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.entity.SalaryDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface SalaryDetailRepository extends JpaRepository<SalaryDetail, UUID>, SalaryDetailRepositoryCustom {
    List<SalaryDetail> findBySalaryIdInAndSalaryNameIn(List<UUID> salaryIdList, Set<String> deductionNames);

    List<SalaryDetail> findBySalaryIdInAndSalaryName(List<UUID> salaryIdList, String salaryName);
}
