package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BreakRuleDto {
    // 휴게 시간 적용 방식: AUTO (근무시간 비례 자동 차감), MANUAL (휴게 시작/종료 직접 기록)
    private String type;
    // 'AUTO'일 경우, 8시간 근무 시 차감할 기본 휴게 시간(분)
    private Integer defaultBreakMinutesFor8Hours;
}
