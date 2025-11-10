package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkTimeRuleDto {
    private String type; // FIXED, FLEXIBLE, DEEMED
    private Integer fixedWorkMinutes;
    private String workStartTime; // 정규 출근 시각 (HH:mm 형식, 예: "09:00")
    private String workEndTime;   // 정규 퇴근 시각 (HH:mm 형식, 예: "18:00")
    private String coreTimeStart;
    private String coreTimeEnd;
    private Integer mandatoryBreakMinutes; // 4시간 근무 시 30분 등 법정 최소 휴게시간
}
