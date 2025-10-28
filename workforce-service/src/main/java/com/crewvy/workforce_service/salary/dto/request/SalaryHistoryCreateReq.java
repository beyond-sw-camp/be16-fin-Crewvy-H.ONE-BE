package com.crewvy.workforce_service.salary.dto.request;

import com.crewvy.workforce_service.salary.constant.PayType;
import com.crewvy.workforce_service.salary.entity.SalaryHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalaryHistoryCreateReq {

    private UUID memberId;
    private PayType payType;
    private int baseSalary;
    private int customaryWage;
    private LocalDate effectiveDate;

    public SalaryHistory toEntity() {
        return SalaryHistory.builder()
                .memberId(memberId)
                .payType(payType)
                .baseSalary(baseSalary)
                .customaryWage(customaryWage)
                .effectiveDate(effectiveDate)
                .build();
    }

}
