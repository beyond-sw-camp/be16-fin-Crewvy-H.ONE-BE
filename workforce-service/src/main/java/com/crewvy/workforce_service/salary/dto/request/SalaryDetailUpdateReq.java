package com.crewvy.workforce_service.salary.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryDetailUpdateReq {

    private UUID detailId;
    private String salaryName;
    private String salaryType;
    private BigInteger amount;
}

