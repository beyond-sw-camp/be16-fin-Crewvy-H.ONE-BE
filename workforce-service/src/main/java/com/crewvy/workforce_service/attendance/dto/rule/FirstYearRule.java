package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1년 미만 근로자 연차 규칙
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirstYearRule {
    /**
     * 월별 연차 발생 여부
     * 근로기준법 제60조: 1개월 개근 시 1일 발생
     */
    private Boolean monthlyAccrualEnabled;

    /**
     * 월별 발생 일수
     * 기본값: 1일
     */
    private Double monthlyAccrualDays;

    /**
     * 연차 발생을 위한 최소 출근율 (%)
     * 예: 80 → 80% 출근율 필요
     * TODO: 출근율 계산 로직 구현 필요
     */
    private Double minimumAttendanceRateForAccrual;

    /**
     * 1년 미만 최대 발생 일수
     * 근로기준법: 최대 11일
     */
    private Integer maxAccrualFirstYear;

    /**
     * 1년 미만 연차 이월 가능 여부
     * true: 이월 가능
     */
    private Boolean carryOverEnabledForFirstYear;

    /**
     * 1년 미만 연차 이월 한도
     * 예: 11일
     */
    private Integer carryOverLimitForFirstYear;
}
