package com.crewvy.workforce_service.salary.dto.response;

import com.crewvy.workforce_service.salary.entity.SalaryInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryInfoRes {

    private UUID id;
    private UUID companyId;
    private UUID memberId;
    private UUID payrollItemId;
    private String payrollItemName;
    private BigInteger amount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SalaryInfoRes from(SalaryInfo salaryInfo) {
        return SalaryInfoRes.builder()
                .id(salaryInfo.getId())
                .companyId(salaryInfo.getCompanyId())
                .memberId(salaryInfo.getMemberId())
                .payrollItemId(salaryInfo.getPayrollItem().getId())
                .payrollItemName(salaryInfo.getPayrollItem().getName())
                .amount(salaryInfo.getAmount())
                .startDate(salaryInfo.getStartDate())
                .endDate(salaryInfo.getEndDate())
                .createdAt(salaryInfo.getCreatedAt())
                .updatedAt(salaryInfo.getUpdatedAt())
                .build();
    }
}
