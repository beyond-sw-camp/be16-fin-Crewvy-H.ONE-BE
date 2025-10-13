package com.crewvy.workforce_service.salary.dto.request;

import com.crewvy.workforce_service.salary.entity.PayrollItem;
import com.crewvy.workforce_service.salary.entity.SalaryInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalaryInfoCreateReq {

    private UUID memberId;
    private UUID companyId;
    private UUID payrollItemId;
    private BigInteger amount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public SalaryInfo toEntity(PayrollItem payrollItem) {
        return SalaryInfo.builder()
                .companyId(companyId)
                .memberId(memberId)
                .payrollItem(payrollItem)
                .amount(amount)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
