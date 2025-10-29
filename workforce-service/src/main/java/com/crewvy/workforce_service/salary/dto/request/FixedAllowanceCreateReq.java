package com.crewvy.workforce_service.salary.dto.request;

import com.crewvy.workforce_service.salary.entity.FixedAllowance;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
public class FixedAllowanceCreateReq {

    private UUID companyId;
    private UUID memberId;
    private String allowanceName;
    private int amount;
    private LocalDate effectiveDate;

    public FixedAllowance toEntity() {
        return FixedAllowance.builder()
                .companyId(companyId)
                .memberId(memberId)
                .allowanceName(allowanceName)
                .amount(amount)
                .effectiveDate(effectiveDate)
                .build();
    }
}
