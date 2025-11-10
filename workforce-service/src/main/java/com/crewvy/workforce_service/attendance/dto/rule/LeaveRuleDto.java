package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Data;

import java.util.List;


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
     * 사후 신청 허용 여부.
     * true: 휴가 시작일 이후에도 신청 가능 (급한 개인 사정 등)
     * false: 사전 신청만 가능
     */
    private Boolean allowRetrospectiveRequest;

    /**
     * 사후 신청 가능 기간 (휴가 시작일로부터 N일 이내).
     * 예: 7일 → 휴가 시작일로부터 최대 7일 이내에만 사후 신청 가능
     * allowRetrospectiveRequest가 true일 때만 사용됩니다.
     */
    private Integer retrospectiveRequestDays;

    /**
     * 휴가 신청 최소 단위 (예: "DAY", "HALF_DAY", "HOUR").
     * @deprecated allowedRequestUnits 필드로 대체 예정
     */
    private String minimumRequestUnit;

    /**
     * 허용되는 휴가 신청 단위 목록.
     * 예: ["DAY", "HALF_DAY_AM", "HALF_DAY_PM"]
     * 이 필드가 설정되면, minimumRequestUnit은 무시됩니다.
     */
    private List<String> allowedRequestUnits;

    /**
     * 1회 최대 신청 가능 일수.
     * 예: 출산휴가 90일, 연차 15일 등
     */
    private Integer maxDaysPerRequest;

    // ========== 연차 전용 필드 (nullable) ==========

    /**
     * 휴가 발생 유형. (예: "ACCRUAL" - 자동 발생, "MANUAL" - 수동 부여)
     * 연차유급휴가에만 사용됩니다.
     */
    private String accrualType;

    /**
     * 연차 발생 방식
     * - LEGAL_STANDARD: 근로기준법 기준 (15일 + 2년마다 1일, 최대 25일)
     * - FIXED: 고정 일수 (근속과 무관하게 동일)
     * - CUSTOM_SENIORITY: 회사 자체 근속 기준
     * @deprecated annualLeaveRule 사용 권장
     */
    @Deprecated
    private String accrualMethod;

    // ========== 연차 규칙 (신규 구조) ==========

    /**
     * 연차 기준 유형
     * - FISCAL_YEAR: 회계연도 기준 (예: 1월 1일 ~ 12월 31일)
     * - JOIN_DATE: 입사일 기준
     */
    private String standardType;

    /**
     * 1년 이상 근로자 기본 연차 일수
     * 근로기준법: 15일
     */
    private Integer baseAnnualLeaveForOverOneYear;

    /**
     * 연차 가산 규칙 목록
     * 예: [{ afterYears: 3, additionalDays: 1 }, { afterYears: 5, additionalDays: 1 }]
     */
    private List<AdditionalAnnualLeaveRule> additionalAnnualLeaveRules;

    /**
     * 최대 연차 일수
     * 근로기준법: 최대 25일
     */
    private Integer maximumAnnualLeaveLimit;

    /**
     * 1년 미만 근로자 규칙
     */
    private FirstYearRule firstYearRule;

    /**
     * 1년 이상 근로자 규칙
     */
    private OverOneYearRule overOneYearRule;

    // ========== 1년 미만 근로자 설정 (Deprecated) ==========

    /**
     * 월별 연차 발생 여부 (1년 미만 근로자)
     * true: 매월 개근 시 연차 발생
     * false: 1년 미만 근로자 연차 미발생
     * @deprecated firstYearRule 사용 권장
     */
    @Deprecated
    private Boolean enableMonthlyAccrual;

    /**
     * 월별 발생 일수
     * 근로기준법: 1일 (기본값)
     * @deprecated firstYearRule 사용 권장
     */
    @Deprecated
    private Double monthlyAccrualDays;

    /**
     * 1년 미만 근로자의 최대 발생 가능 연차 일수.
     * 근로기준법 제60조 제2항: 1개월 개근 시 1일 (최대 11일)
     * 연차유급휴가에만 사용됩니다.
     * @deprecated firstYearRule 사용 권장
     */
    @Deprecated
    private Integer firstYearMaxAccrual;

    // ========== 출근율 기반 연차 부여 조건 ==========

    /**
     * 출근율 체크 여부 (1년 이상 근로자용)
     * 근로기준법 제60조 제2항: 1년간 80% 이상 출근 시 15일 부여
     */
    private Boolean enableAttendanceRateCheck;

    /**
     * 최소 요구 출근율 (예: 80.0 → 80%)
     * 기본값: 80.0
     */
    private Double minimumAttendanceRate;

    /**
     * 출근으로 인정되는 항목들
     * 예: ["WORKING", "BUSINESS_TRIP", "REMOTE_WORK", "EDUCATION"]
     * 고용노동부 해석: 출장, 교육, 재택근무는 출근 인정
     */
    private List<String> countAsAttendance;

    // ========== 연차 가산 설정 (Deprecated) ==========

    /**
     * 연차 가산 여부
     * true: 근속연수에 따라 연차 가산
     * false: 가산 없음
     * @deprecated additionalAnnualLeaveRules 사용 권장
     */
    @Deprecated
    private Boolean enableAdditionalLeave;

    /**
     * 가산 시작 근속 연차 (예: 3 → 3년차부터)
     * 근로기준법: 3년차부터 가산
     * @deprecated additionalAnnualLeaveRules 사용 권장
     */
    @Deprecated
    private Integer additionalLeaveStartYear;

    /**
     * 가산 주기 (예: 2 → 2년마다)
     * 근로기준법: 2년마다 1일 가산
     * @deprecated additionalAnnualLeaveRules 사용 권장
     */
    @Deprecated
    private Integer additionalLeaveInterval;

    /**
     * 가산 일수 (예: 1.0 → 주기마다 1일씩)
     * @deprecated additionalAnnualLeaveRules 사용 권장
     */
    @Deprecated
    private Double additionalLeaveDaysPerInterval;

    /**
     * 최대 연차 일수 (예: 25)
     * 근로기준법: 최대 25일
     * @deprecated maximumAnnualLeaveLimit 사용 권장
     */
    @Deprecated
    private Integer maxAnnualLeaveDays;

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
