package com.crewvy.workforce_service.salary.dto.request;

import com.crewvy.workforce_service.salary.constant.SalaryStatus;
import com.crewvy.workforce_service.salary.entity.Salary;
import com.crewvy.workforce_service.salary.entity.SalaryDetail;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SalaryCreateReq {

    private UUID memberId;
    private String salaryName;
    private BigInteger totalAllowance;
    private BigInteger totalDeduction;
    private BigInteger netPay;
    private LocalDate paymentDate;
    private List<SalaryDetail> allowanceList;
    private List<SalaryDetail> deductionList;

    public Salary toEntity(UUID companyId) {
        return Salary.builder()
                .companyId(companyId)
                .memberId(memberId)
                .salaryStatus(SalaryStatus.PENDING)
                .totalAllowance(totalAllowance)
                .totalDeduction(totalDeduction)
                .netPay(netPay)
                .paymentDate(paymentDate)
                .build();
    }
}
