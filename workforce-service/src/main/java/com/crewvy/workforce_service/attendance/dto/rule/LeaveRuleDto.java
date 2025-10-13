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
}
