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
    private BigInteger amount;  // 총 급여
    private BigInteger netPay;  // 실수령액
    private LocalDate paymentDate;  // 지급일
    private List<SalaryDetailUpdateReq> detailList;  // 상세 내역
}

