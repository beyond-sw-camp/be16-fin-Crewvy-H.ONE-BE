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
public interface SalaryHistoryRepository extends JpaRepository<SalaryHistory, UUID> {

    List<SalaryHistory> findAllByMemberId(UUID memberId);

    @Query("SELECT sh FROM SalaryHistory sh " +
            "WHERE sh.companyId = :companyId " +
            "  AND sh.effectiveDate <= :targetDate " +
            "  AND sh.effectiveDate = ( " +
            "    SELECT MAX(sh2.effectiveDate) " +
            "    FROM SalaryHistory sh2 " +
            "    WHERE sh2.memberId = sh.memberId " +
            "      AND sh2.effectiveDate <= :targetDate " +
            "  )")
    List<SalaryHistory> findLatestSalaryHistoriesByCompany(
            @Param("companyId") UUID companyId,
            @Param("targetDate") LocalDate targetDate
    );

}
