package com.crewvy.workforce_service.attendance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class PolicyUpdateRequest {

    @NotNull(message = "정책 유형 ID는 필수입니다.")
    private UUID policyTypeId;

    @NotBlank(message = "정책 이름은 필수입니다.")
    private String name;

    @NotNull(message = "유급 여부는 필수입니다.")
    private Boolean isPaid;

    @NotNull(message = "정책 시작일은 필수입니다.")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private Map<String, Object> ruleDetails;
}
