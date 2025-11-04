package com.crewvy.workforce_service.salary.dto.request;

import com.crewvy.workforce_service.salary.constant.SalaryType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
public class SalaryDetailCreateReq {
    private SalaryType salaryType;
    private String salaryName;
    private BigInteger amount;
}
