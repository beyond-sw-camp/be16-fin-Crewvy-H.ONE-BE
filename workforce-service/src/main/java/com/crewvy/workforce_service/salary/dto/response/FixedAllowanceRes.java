package com.crewvy.workforce_service.salary.dto.response;

import com.crewvy.workforce_service.salary.entity.FixedAllowance;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedAllowanceRes {

    private UUID companyId;
    private UUID memberId;
    private String allowanceName;
    private int amount;
    private LocalDate effectiveDate;

    public static FixedAllowanceRes fromEntity(FixedAllowance fixedAllowance) {
        return FixedAllowanceRes.builder()
                .companyId(fixedAllowance.getCompanyId())
                .memberId(fixedAllowance.getMemberId())
                .allowanceName(fixedAllowance.getAllowanceName())
                .amount(fixedAllowance.getAmount())
                .effectiveDate(fixedAllowance.getEffectiveDate())
                .build();
    }
}
