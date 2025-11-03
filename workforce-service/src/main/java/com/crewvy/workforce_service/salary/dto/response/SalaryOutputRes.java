package com.crewvy.workforce_service.salary.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@Builder
public class SalaryOutputRes {

    private String bank;
    private String bankAccount;
    private String sabun;
    private String memberName;
    private String department;
    private BigInteger netPay;

}
