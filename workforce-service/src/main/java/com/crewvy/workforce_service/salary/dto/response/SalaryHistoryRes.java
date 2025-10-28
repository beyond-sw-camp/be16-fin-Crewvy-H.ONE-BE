package com.crewvy.workforce_service.salary.dto.response;

import com.crewvy.workforce_service.salary.constant.PayType;
import com.crewvy.workforce_service.salary.entity.SalaryHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryHistoryRes {

    private PayType payType;
    private int baseSalary;
    private int customaryWage;
    private LocalDate effectiveDate;

    public static SalaryHistoryRes fromEntity(SalaryHistory salaryHistory) {

        return SalaryHistoryRes.builder()
                .payType(salaryHistory.getPayType())
                .baseSalary(salaryHistory.getBaseSalary())
                .customaryWage(salaryHistory.getCustomaryWage())
                .effectiveDate(salaryHistory.getEffectiveDate())
                .build();

    }

}
