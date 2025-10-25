package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Data;


/**
 * 휴가 정책에 대한 규칙을 정의하는 DTO.
 * 모든 휴가 타입(연차, 출산전후휴가, 육아휴직 등)을 지원합니다.
 */
@Data
public class LeaveRuleDto {
    // ========== 공통 필드 ==========

    /**
     * 기본 부여 일수.
     * - 연차: 근로기준법 제60조 (15일 이상)
     * - 출산전후휴가: 근로기준법 제74조 (90일 이상)
     * - 배우자출산휴가: 남녀고용평등법 제18조의2 (10일 이상)
     * - 육아휴직: 남녀고용평등법 제19조 (최대 365일)
     * - 가족돌봄휴가: 남녀고용평등법 제22조의2 (연간 최대 10일)
     * - 생리휴가: 근로기준법 제73조 (월 1일)
     */
    private Double defaultDays;

    /**
     * 휴가 신청 마감일 (휴가 시작일로부터 N일 전).
     */
    private Integer requestDeadlineDays;

    /**
     * 휴가 신청 최소 단위 (예: "DAY", "HALF_DAY", "HOUR").
     */
    private String minimumRequestUnit;

    // ========== 연차 전용 필드 (nullable) ==========

    /**
     * 휴가 발생 유형. (예: "ACCRUAL" - 자동 발생, "MANUAL" - 수동 부여)
     * 연차유급휴가에만 사용됩니다.
     */
    private String accrualType;

    /**
     * 1년 미만 근로자의 최대 발생 가능 연차 일수.
     * 근로기준법 제60조 제2항: 1개월 개근 시 1일 (최대 11일)
     * 연차유급휴가에만 사용됩니다.
     */
    private Integer firstYearMaxAccrual;

    // ========== 주기별 제한 필드 (nullable) ==========

    /**
     * 사용 제한 주기. (예: "MONTHLY", "YEARLY")
     * - 생리휴가: "MONTHLY" (월 1일)
     * - 가족돌봄휴가: "YEARLY" (연 10일)
     */
    private String limitPeriod;

    /**
     * 주기당 최대 사용 가능 일수.
     * limitPeriod와 함께 사용됩니다.
     */
    private Integer maxDaysPerPeriod;

    // ========== 분할 사용 규칙 (nullable) ==========

    /**
     * 최대 분할 사용 횟수.
     * 육아휴직 등 장기 휴가에 사용됩니다.
     */
    private Integer maxSplitCount;

    /**
     * 최소 연속 사용 일수.
     * 분할 사용 시 1회당 최소 며칠을 사용해야 하는지 지정합니다.
     */
    private Integer minConsecutiveDays;

    // ========== 특수 제한 필드 (nullable) ==========

    /**
     * 신청 가능한 기준일로부터 최대 허용 일수.
     * 예: 배우자 출산휴가는 출산일 기준 ±90일 이내 사용
     */
    private Integer maxDaysFromEventDate;
}
