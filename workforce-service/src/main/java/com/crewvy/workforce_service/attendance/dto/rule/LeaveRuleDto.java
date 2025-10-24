package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Data;


/**
 * 휴가 정책에 대한 규칙을 정의하는 DTO.
 */
@Data
public class LeaveRuleDto {
    /**
     * 휴가 발생 유형. (예: ACCRUAL - 자동 발생, MANUAL - 수동 부여)
     */
    private String accrualType;

    /**
     * 기본 부여 일수.
     * 근로기준법 제60조 제1항에 따라, 1년간 80% 이상 출근한 근로자에게는 15일의 유급휴가를 주어야 합니다.
     */
    private Double defaultDays;

    /**
     * 1년 미만 근로자의 최대 발생 가능 연차 일수.
     * 근로기준법 제60조 제2항에 따라, 1개월 개근 시 1일의 유급휴가를 주어야 하므로, 1년 미만 근로자는 최대 11일까지 발생 가능합니다.
     */
    private Integer firstYearMaxAccrual;

    /**
     * 휴가 신청 마감일 (휴가 시작일로부터 N일 전).
     */
    private Integer requestDeadlineDays;

    /**
     * 휴가 신청 최소 단위 (예: DAY, HALF_DAY).
     */
    private String minimumRequestUnit;
}
