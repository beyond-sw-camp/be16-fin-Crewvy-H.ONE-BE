package com.crewvy.workforce_service.salary.dto.response;

import com.crewvy.common.entity.Bool;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollItemRes {

    private UUID id;
    private UUID companyId;
    private UUID memberId;
    private SalaryType salaryType;
    private String name;
    private Bool isActive;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PayrollItemRes from(PayrollItem payrollItem) {
        return PayrollItemRes.builder()
                .id(payrollItem.getId())
                .companyId(payrollItem.getCompanyId())
                .memberId(payrollItem.getMemberId())
                .salaryType(payrollItem.getSalaryType())
                .name(payrollItem.getName())
                .isActive(payrollItem.getIsActive())
                .description(payrollItem.getDescription())
                .createdAt(payrollItem.getCreatedAt())
                .updatedAt(payrollItem.getUpdatedAt())
                .build();
    }
}
