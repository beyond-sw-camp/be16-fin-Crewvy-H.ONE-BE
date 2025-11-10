package com.crewvy.workforce_service.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ClockOutResponse {
    private UUID attendanceLogId;
    private LocalDateTime eventTime;
    private Integer totalWorkedMinutes;    // 총 근무 시간(분)
    private Integer totalOvertimeMinutes;  // 총 초과 근무 시간(분)

    // 조퇴 정보
    private Integer earlyLeaveMinutes;     // 조퇴 시간(분) - 정규 퇴근시간 기준
    private Boolean isEarlyLeave;          // 허용 시간 초과 여부 (급여 차감 대상)

    // 월별 조퇴 허용 횟수 정보
    private Integer monthlyEarlyLeaveCount; // 이번 달 조퇴 횟수 (허용 시간 내 포함)
    private Integer monthlyAllowedCount;    // 월별 허용 횟수 (null이면 무제한)
}
