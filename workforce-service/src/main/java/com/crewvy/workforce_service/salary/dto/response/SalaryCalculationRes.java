package com.crewvy.workforce_service.salary.dto.response;

import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.entity.Salary;
import com.crewvy.workforce_service.salary.entity.SalaryDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryCalculationRes {
    private UUID salaryId;
    private UUID memberId;
    private String memberName;
    private String department;
    private int workingDays;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private LocalDate paymentDate;
    private List<SalaryDetailRes> allowanceList;
    private List<SalaryDetailRes> deductionList;
    private BigInteger totalAllowance;
    private BigInteger totalDeduction;
    private BigInteger netPay;
    
    public static SalaryCalculationRes fromEntity(Salary salary, String memberName, String department,
                                                int workingDays, LocalDate periodStartDate, LocalDate periodEndDate) {
        List<SalaryDetailRes> allowanceList = new ArrayList<>();
        List<SalaryDetailRes> deductionList = new ArrayList<>();
        
        BigInteger totalAllowance = BigInteger.ZERO;
        BigInteger totalDeduction = BigInteger.ZERO;
        
        for (SalaryDetail detail : salary.getSalaryDetailList()) {
            SalaryDetailRes detailRes = SalaryDetailRes.builder()
                    .salaryName(detail.getSalaryName())
                    .salaryType(detail.getSalaryType().name())
                    .amount(detail.getAmount())
                    .build();
            
            if (detail.getSalaryType() == SalaryType.ALLOWANCE) {
                allowanceList.add(detailRes);
                totalAllowance = totalAllowance.add(detail.getAmount());
            } else {
                deductionList.add(detailRes);
                totalDeduction = totalDeduction.add(detail.getAmount());
            }
        }
        
        return SalaryCalculationRes.builder()
                .salaryId(salary.getId())
                .memberId(salary.getMemberId())
                .memberName(memberName != null ? memberName : "")
                .department(department != null ? department : "")
                .workingDays(workingDays)
                .periodStartDate(periodStartDate)
                .periodEndDate(periodEndDate)
                .paymentDate(salary.getPaymentDate())
                .allowanceList(allowanceList)
                .deductionList(deductionList)
                .totalAllowance(totalAllowance)
                .totalDeduction(totalDeduction)
                .netPay(salary.getNetPay())
                .build();
    }
}
