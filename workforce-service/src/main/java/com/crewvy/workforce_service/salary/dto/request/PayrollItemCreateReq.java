package com.crewvy.workforce_service.salary.dto.request;

import com.crewvy.common.entity.Bool;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayrollItemCreateReq {

    private UUID companyId;
    private UUID memberId;
    private SalaryType salaryType;
    private String name;
    private Bool isActive;
    private String description;

    public PayrollItem toEntity() {
        return PayrollItem.builder()
                .companyId(companyId)
                .memberId(memberId)
                .salaryType(salaryType)
                .name(name)
                .isActive(isActive)
                .description(description)
                .build();
    }
}
