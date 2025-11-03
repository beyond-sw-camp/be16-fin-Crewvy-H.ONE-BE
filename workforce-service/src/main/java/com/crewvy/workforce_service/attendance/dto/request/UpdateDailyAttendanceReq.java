package com.crewvy.workforce_service.attendance.dto.request;

import com.crewvy.workforce_service.attendance.constant.AttendanceStatus;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDailyAttendanceReq {

    private LocalDateTime firstClockIn;

    private LocalDateTime lastClockOut;

    @Min(value = 0, message = "근무 시간은 0분 이상이어야 합니다")
    private Integer workedMinutes;

    @Min(value = 0, message = "초과 근무 시간은 0분 이상이어야 합니다")
    private Integer overtimeMinutes;

    @Min(value = 0, message = "휴게 시간은 0분 이상이어야 합니다")
    private Integer totalBreakMinutes;

    @Min(value = 0, message = "외출 시간은 0분 이상이어야 합니다")
    private Integer totalGoOutMinutes;

    private Boolean isLate;

    @Min(value = 0, message = "지각 시간은 0분 이상이어야 합니다")
    private Integer lateMinutes;

    private Boolean isEarlyLeave;

    @Min(value = 0, message = "조퇴 시간은 0분 이상이어야 합니다")
    private Integer earlyLeaveMinutes;

    private AttendanceStatus status;

    private String adminComment; // 관리자 수정 사유
}
