package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.entity.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, UUID> {

    List<Salary> findByCompanyIdAndPaymentDateBetween(UUID companyId, java.time.LocalDate startDate, java.time.LocalDate endDate);
    
    List<Salary> findByCompanyIdOrderByPaymentDateDesc(UUID companyId);
    
    List<Salary> findByCompanyIdAndMemberIdOrderByPaymentDateDesc(UUID companyId, UUID memberId);
    
    Salary findFirstByCompanyIdAndMemberIdOrderByPaymentDateDesc(UUID companyId, UUID memberId);
}
