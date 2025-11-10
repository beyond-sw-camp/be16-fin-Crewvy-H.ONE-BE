package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 연차 가산 규칙
 * 근속 연수에 따른 추가 연차 부여 규칙
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalAnnualLeaveRule {
    /**
     * N년차부터 가산
     * 예: 3 → 3년차부터
     */
    private Integer afterYears;

    /**
     * 추가 부여 일수
     * 예: 1 → 1일 추가
     */
    private Double additionalDays;
}
