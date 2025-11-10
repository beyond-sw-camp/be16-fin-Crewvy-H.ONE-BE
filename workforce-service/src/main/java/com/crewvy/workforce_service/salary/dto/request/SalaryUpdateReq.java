package com.crewvy.workforce_service.salary.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryUpdateReq {
    private UUID salaryId;
    private BigInteger totalAllowance;
    private BigInteger totalDeduction;
    private BigInteger netPay;
    private LocalDate paymentDate;
    private List<SalaryDetailUpdateReq> detailList;
}

