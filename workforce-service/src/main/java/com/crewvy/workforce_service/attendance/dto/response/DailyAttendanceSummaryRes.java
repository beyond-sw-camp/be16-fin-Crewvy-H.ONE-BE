package com.crewvy.workforce_service.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAttendanceSummaryRes {
    private UUID memberId;
    private LocalDate attendanceDate;
    private LocalDateTime firstClockIn;
    private LocalDateTime lastClockOut;
    private Integer workedMinutes;
    private Integer overtimeMinutes;
}
