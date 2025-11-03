package com.crewvy.workforce_service.attendance.dto.request;

import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Getter
@NoArgsConstructor
public class PolicyUpdateRequest {

    @NotNull(message = "정책 유형 코드는 필수입니다.")
    private PolicyTypeCode typeCode;

    @NotBlank(message = "정책 이름은 필수입니다.")
    private String name;
    @NotNull(message = "유급 여부는 필수입니다.")
    private Boolean isPaid;

    @NotNull(message = "정책 시작일은 필수입니다.")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    /**
     * 자동 승인 여부.
     */
    private Boolean autoApprove;

    private Map<String, Object> ruleDetails;
}
