package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.entity.SalaryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SalaryHistoryRepository extends JpaRepository<SalaryHistory, UUID>, SalaryHistoryRepositoryCustom {

    List<SalaryHistory> findAllByMemberId(UUID memberId);

}
