package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveRuleDto {
    // 예: 연차, 병가, 경조사휴가 등 휴가 유형
    private String type;
    // 기본 부여 일수
    private Integer defaultDays;
    // 연차 이월 가능 여부
    private Boolean allowCarryOver;
    // 연차 발생 유형 (YEARLY: 회계연도 기준, ENTRY_DATE: 입사일 기준)
    private String accrualType;
    // 1년 미만 근속자 최대 발생 연차 (법적 기준 11일)
    private Integer firstYearMaxAccrual;
}
