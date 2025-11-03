package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.constant.SalaryStatus;
import com.crewvy.workforce_service.salary.entity.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, UUID> {

    List<Salary> findByCompanyIdAndSalaryStatusNotAndPaymentDate(
            UUID companyId,
            SalaryStatus salaryStatus,
            LocalDate paymentDate

    );
    
    List<Salary> findByCompanyIdAndMemberIdOrderByPaymentDateDesc(UUID companyId, UUID memberId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Salary s " +
            "WHERE s.memberId IN :memberIds " +
            "AND s.paymentDate BETWEEN :startDate AND :endDate " +
            "AND s.salaryStatus = :status")
    boolean existsSalary(@Param("memberIds") List<UUID> memberIds,
                         @Param("startDate") LocalDate startDate,
                         @Param("endDate") LocalDate endDate,
                         @Param("status") SalaryStatus status);

    @Query("SELECT s FROM Salary s " +
            "WHERE s.memberId IN :memberIds " +
            "AND s.paymentDate BETWEEN :startDate AND :endDate " +
            "AND s.salaryStatus = :status")
    List<Salary> findAllActiveSalaries(@Param("memberIds") List<UUID> memberIds,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("status") SalaryStatus status);

    List<Salary> findByMemberIdInAndPaymentDate(List<UUID> memberIds, LocalDate paymentDate);
}
