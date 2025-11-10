package com.crewvy.workforce_service.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ClockInResponse {

    private UUID attendanceLogId;
    private LocalDateTime eventTime;

    // 지각 정보
    private Integer lateMinutes;           // 지각 시간(분) - 정규 출근시간 기준
    private Boolean isLate;                // 허용 시간 초과 여부 (급여 차감 대상)

    // 월별 지각 허용 횟수 정보
    private Integer monthlyLatenessCount;  // 이번 달 지각 횟수 (허용 시간 내 포함)
    private Integer monthlyAllowedCount;   // 월별 허용 횟수 (null이면 무제한)
}
