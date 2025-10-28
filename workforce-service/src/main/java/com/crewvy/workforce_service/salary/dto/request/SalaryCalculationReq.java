package com.crewvy.workforce_service.salary.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.YearMonth;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalaryCalculationReq {
    private UUID companyId;
    private YearMonth yearMonth;
}
