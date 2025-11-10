package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 연장, 야간, 휴일 근무에 대한 규칙을 정의하는 DTO.
 * 이 규칙들은 근로기준법의 가산임금 및 근로시간 제한 조항을 기반으로 합니다.
 */
@Data
public class OvertimeRuleDto {

    /**
     * 주간 최대 연장근무 한도 (분 단위).
     * 근로기준법 제53조에 따라 주 12시간(720분)을 초과할 수 없습니다.
     */
    private Integer maxWeeklyOvertimeMinutes;

    /**
     * 연장근무에 대한 가산임금률.
     * 근로기준법 제56조 제1항에 따라 통상임금의 100분의 50 이상을 가산해야 합니다 (값: 1.5 이상).
     */
    private BigDecimal overtimeRate;

    /**
     * 야간근무(22:00 ~ 06:00)에 대한 가산임금률.
     * 근로기준법 제56조 제3항에 따라 통상임금의 100분의 50 이상을 가산해야 합니다 (값: 1.5 이상).
     * 연장근무와 중복 시 합산 적용됩니다.
     */
    private BigDecimal nightWorkRate;

    /**
     * 8시간 이내의 휴일근무에 대한 가산임금률.
     * 근로기준법 제56조 제2항에 따라 통상임금의 100분의 50 이상을 가산해야 합니다 (값: 1.5 이상).
     */
    private BigDecimal holidayWorkRate;

    /**
     * 8시간을 초과하는 휴일근무에 대한 가산임금률.
     * 근로기준법 제56조 제2항에 따라 통상임금의 100분의 100 이상을 가산해야 합니다 (값: 2.0 이상).
     */
    private BigDecimal holidayOvertimeRate;
}