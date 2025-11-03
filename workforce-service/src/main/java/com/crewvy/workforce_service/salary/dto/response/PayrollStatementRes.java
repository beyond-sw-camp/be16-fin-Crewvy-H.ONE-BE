package com.crewvy.workforce_service.salary.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.time.YearMonth;
import java.util.UUID;

@Getter
@Setter
@Builder
public class PayrollStatementRes {
    private UUID memberId;
    private String memberName;
    private YearMonth salaryPeriod;
    private BigInteger basePay;
    private BigInteger allowance;
    private BigInteger incomeTax;
    private BigInteger fourInsurances;
    private BigInteger totalPayment;
    private BigInteger netPay;
    private String status;
}
