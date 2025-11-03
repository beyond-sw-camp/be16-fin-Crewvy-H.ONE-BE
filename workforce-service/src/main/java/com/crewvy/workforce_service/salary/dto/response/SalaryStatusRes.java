package com.crewvy.workforce_service.salary.dto.response;

import com.crewvy.workforce_service.salary.constant.SalaryStatus;
import com.crewvy.workforce_service.salary.entity.Salary;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class SalaryStatusRes {
    private String memberName;
    private String department;
    private String role;
    private BigInteger baseSalary;
    private BigInteger totalAllowance;
    private BigInteger totalDeduction;
    private BigInteger netPay;
    private LocalDate paymentDate;
    private SalaryStatus status;

    public static SalaryStatusRes fromEntity(Salary salary) {
        return SalaryStatusRes.builder()
                .baseSalary(salary.getBaseSalary())
                .totalAllowance(salary.getTotalAllowance())
                .totalDeduction(salary.getTotalDeduction())
                .netPay(salary.getNetPay())
                .paymentDate(salary.getPaymentDate())
                .status(salary.getSalaryStatus())
                .build();
    }

}
