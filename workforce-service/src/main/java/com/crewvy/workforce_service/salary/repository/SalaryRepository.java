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
public interface SalaryRepository extends JpaRepository<Salary, UUID>, SalaryRepositoryCustom {

    List<Salary> findByCompanyIdAndSalaryStatusNotAndPaymentDate(
            UUID companyId,
            SalaryStatus salaryStatus,
            LocalDate paymentDate

    );
    
    List<Salary> findByCompanyIdAndMemberIdOrderByPaymentDateDesc(UUID companyId, UUID memberId);

    List<Salary> findByMemberIdInAndPaymentDate(List<UUID> memberIds, LocalDate paymentDate);

    List<Salary> findAllByStatusAndPaymentDateBefore(SalaryStatus salaryStatus, LocalDate paymentDate);
}
