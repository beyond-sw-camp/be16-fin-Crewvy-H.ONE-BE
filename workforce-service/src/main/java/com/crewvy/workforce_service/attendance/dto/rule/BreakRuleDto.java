package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Getter;
import lombok.Setter;

/**
 * 휴게 정책 규칙
 */
@Getter
@Setter
public class BreakRuleDto {
    // 휴게 시간 적용 방식: AUTO (자동 차감), MANUAL (직접 기록), FIXED (고정 시간)
    private String type;

    // 'AUTO'일 경우, 8시간 근무 시 차감할 기본 휴게 시간(분)
    private Integer defaultBreakMinutesFor8Hours;

    // 법정 최소 휴게 시간(분)
    private Integer mandatoryBreakMinutes;

    // 'FIXED'일 경우, 고정 휴게 시작 시각 (HH:mm 형식, 예: "12:00")
    private String fixedBreakStart;

    // 'FIXED'일 경우, 고정 휴게 종료 시각 (HH:mm 형식, 예: "13:00")
    private String fixedBreakEnd;

    // 일일 최대 휴게 시간 (분) - null이면 제한 없음
    private Integer maxDailyBreakMinutes;
}
