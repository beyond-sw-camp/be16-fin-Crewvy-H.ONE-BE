package com.crewvy.workforce_service.salary.dto.request;

import com.crewvy.workforce_service.salary.entity.FixedAllowance;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class FixedAllowanceCreateAllReq {

    private String allowanceName;
    private int amount;
    private LocalDate effectiveDate;

    public FixedAllowance toEntity(UUID memberId, UUID companyId) {
        return FixedAllowance.builder()
                .companyId(companyId)
                .memberId(memberId)
                .allowanceName(allowanceName)
                .amount(amount)
                .effectiveDate(effectiveDate)
                .build();
    }
}
