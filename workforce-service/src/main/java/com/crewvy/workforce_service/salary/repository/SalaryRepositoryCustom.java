package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.constant.SalaryStatus;
import com.crewvy.workforce_service.salary.entity.Salary;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SalaryRepositoryCustom {

    boolean existsSalary(List<UUID> memberIds,
                         LocalDate startDate,
                         LocalDate endDate,
                         SalaryStatus status);

    List<Salary> findAllActiveSalaries(List<UUID> memberIds,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       SalaryStatus status);
}
