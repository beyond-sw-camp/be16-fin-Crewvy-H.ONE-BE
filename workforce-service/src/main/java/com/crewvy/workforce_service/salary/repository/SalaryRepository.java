package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.constant.SalaryStatus;
import com.crewvy.workforce_service.salary.entity.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, UUID>, SalaryRepositoryCustom {

    List<Salary> findByCompanyIdAndSalaryStatusNotAndPaymentDate(
            UUID companyId,
            SalaryStatus salaryStatus,
            LocalDate paymentDate

    );


    List<Salary> findByCompanyIdAndSalaryStatusNotAndPaymentDateBetween(UUID companyId,
                                                                       SalaryStatus status,
                                                                       LocalDate startDate,
                                                                       LocalDate endDate);
    
    List<Salary> findByCompanyIdAndMemberIdOrderByPaymentDateDesc(UUID companyId, UUID memberId);

    List<Salary> findByMemberIdInAndPaymentDate(List<UUID> memberIds, LocalDate paymentDate);

    List<Salary> findByMemberIdInAndPaymentDateBetween(List<UUID> memberIds,LocalDate startDate,
                                                       LocalDate endDate);

    List<Salary> findAllBySalaryStatusAndPaymentDateBefore(SalaryStatus salaryStatus, LocalDate paymentDate);
}
