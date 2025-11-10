package com.crewvy.workforce_service.salary.dto.response;

import com.crewvy.common.entity.Bool;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollItemRes {

    private UUID id;
    private UUID companyId;
    private SalaryType salaryType;
    private String name;
    private String calculationCode;
    private Bool isActive;
    private Bool isTaxable;
    private int nonTaxableLimit;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PayrollItemRes fromEntity(PayrollItem payrollItem) {
        return PayrollItemRes.builder()
                .id(payrollItem.getId())
                .companyId(payrollItem.getCompanyId())
                .salaryType(payrollItem.getSalaryType())
                .name(payrollItem.getName())
                .calculationCode(payrollItem.getCalculationCode())
                .isActive(payrollItem.getIsActive())
                .isTaxable(payrollItem.getIsTaxable())
                .nonTaxableLimit(payrollItem.getNonTaxableLimit())
                .description(payrollItem.getDescription())
                .createdAt(payrollItem.getCreatedAt())
                .updatedAt(payrollItem.getUpdatedAt())
                .build();
    }
}