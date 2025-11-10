package com.crewvy.workforce_service.salary.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
public class PayrollDeductionRes {
    private UUID memberId;
    private String memberName;
    private String department;
    private YearMonth period;
    private String status;
    private Map<String, BigInteger> deductionMap;
    private BigInteger totalDeductions;
}
