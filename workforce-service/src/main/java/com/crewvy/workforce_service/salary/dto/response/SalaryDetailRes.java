package com.crewvy.workforce_service.salary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryDetailRes {
    private String salaryName;
    private String salaryType;
    private BigInteger amount;
}
