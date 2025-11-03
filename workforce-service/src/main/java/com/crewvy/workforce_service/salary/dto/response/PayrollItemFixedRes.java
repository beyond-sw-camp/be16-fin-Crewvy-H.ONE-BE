package com.crewvy.workforce_service.salary.dto.response;

import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import lombok.*;

import java.util.UUID;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollItemFixedRes {

    private UUID id;
    private UUID companyId;
    private SalaryType salaryType;
    private String name;
    private String description;

    public static PayrollItemFixedRes fromEntity(PayrollItem payrollItem) {
        return PayrollItemFixedRes.builder()
                .id(payrollItem.getId())
                .companyId(payrollItem.getCompanyId())
                .salaryType(payrollItem.getSalaryType())
                .name(payrollItem.getName())
                .description(payrollItem.getDescription())
                .build();
    }
}
