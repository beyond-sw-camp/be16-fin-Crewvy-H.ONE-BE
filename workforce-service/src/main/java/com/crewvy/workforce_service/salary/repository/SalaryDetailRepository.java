package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.dto.response.ItemTotalRes;
import com.crewvy.workforce_service.salary.entity.SalaryDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface SalaryDetailRepository extends JpaRepository<SalaryDetail, UUID> {
    List<SalaryDetail> findBySalaryIdInAndSalaryNameIn(List<UUID> salaryIdList, Set<String> deductionNames);

    @Query("SELECT new com.crewvy.workforce_service.salary.dto.response.ItemTotalRes(sd.salaryName, SUM(sd.amount)) " +
            "FROM SalaryDetail sd " +
            "WHERE sd.salary.id IN :salaryIdList " +
            "GROUP BY sd.salaryName")
    List<ItemTotalRes> sumAmountsBySalaryName(@Param("salaryIdList") List<UUID> salaryIdList);

    List<SalaryDetail> findBySalaryIdInAndSalaryName(List<UUID> salaryIdList, String salaryName);
}
