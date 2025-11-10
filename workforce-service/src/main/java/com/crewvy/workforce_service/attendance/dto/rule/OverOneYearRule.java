package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1년 이상 근로자 연차 규칙
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverOneYearRule {
    /**
     * 연차 이월 가능 여부
     * true: 미사용 연차 익년으로 이월
     */
    private Boolean carryOverEnabled;

    /**
     * 이월 가능 최대 일수
     * 예: 15일
     */
    private Integer carryOverLimitDays;

    /**
     * 이월 연차 만료 기간 (개월)
     * 예: 3 → 익년 3월 31일까지 사용 가능
     * 근로기준법: 이월 연차는 1년 내 사용 (회사는 더 짧게 설정 가능)
     */
    private Integer carryOverExpirationMonths;
}
