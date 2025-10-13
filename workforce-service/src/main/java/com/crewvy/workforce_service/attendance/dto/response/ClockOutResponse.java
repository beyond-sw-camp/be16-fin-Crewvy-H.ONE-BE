package com.crewvy.workforce_service.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ClockOutResponse {
    private UUID attendanceLogId;
    private LocalDateTime eventTime;
    private Integer totalWorkedMinutes; // 총 근무 시간(분)
    private Integer totalOvertimeMinutes; // 총 초과 근무 시간(분)
}
