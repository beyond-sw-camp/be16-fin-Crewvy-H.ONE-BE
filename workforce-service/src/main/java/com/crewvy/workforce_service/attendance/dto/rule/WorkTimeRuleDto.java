package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkTimeRuleDto {
    private String type; // FIXED, FLEXIBLE, DEEMED
    private Integer fixedWorkMinutes;
    private String coreTimeStart;
    private String coreTimeEnd;
}
