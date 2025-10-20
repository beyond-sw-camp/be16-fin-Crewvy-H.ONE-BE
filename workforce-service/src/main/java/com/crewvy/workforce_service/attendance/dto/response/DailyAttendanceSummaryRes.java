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
    private String memberName;          // 직원명
    private String organizationName;    // 조직명
    private String titleName;           // 직책명
    private LocalDate attendanceDate;
    private String status;              // 근태 상태 코드 (AS001, AS101 등)
    private String statusName;          // 근태 상태 한글명 (정상 출근, 연차 등)
    private Boolean isPaid;             // 유급 여부 (급여 정산용)
    private LocalDateTime firstClockIn;
    private LocalDateTime lastClockOut;
    private Integer workedMinutes;
    private Integer overtimeMinutes;
}
