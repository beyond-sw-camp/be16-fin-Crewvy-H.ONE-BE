package com.crewvy.workforce_service.attendance.dto.response;

import com.crewvy.workforce_service.attendance.constant.AttendanceStatus;
import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 월별 근태 조회 응답 DTO
 * workedMinutes를 항상 최신 값으로 재계산하여 반환
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyAttendanceDto {

    private UUID id;
    private UUID memberId;
    private UUID companyId;
    private LocalDate attendanceDate;
    private AttendanceStatus status;
    private LocalDateTime firstClockIn;
    private LocalDateTime lastClockOut;
    private Integer workedMinutes;  // 재계산된 값
    private Integer overtimeMinutes;
    private Integer daytimeOvertimeMinutes;
    private Integer nightWorkMinutes;
    private Integer holidayWorkMinutes;
    private Integer totalBreakMinutes;
    private Integer totalGoOutMinutes;
    private Boolean isLate;
    private Integer lateMinutes;
    private Boolean isEarlyLeave;
    private Integer earlyLeaveMinutes;

    /**
     * DailyAttendance 엔티티를 DTO로 변환하면서 workedMinutes를 재계산
     */
    public static MonthlyAttendanceDto from(DailyAttendance attendance) {
        // workedMinutes 재계산
        Integer calculatedWorkedMinutes = calculateWorkedMinutes(attendance);

        return MonthlyAttendanceDto.builder()
                .id(attendance.getId())
                .memberId(attendance.getMemberId())
                .companyId(attendance.getCompanyId())
                .attendanceDate(attendance.getAttendanceDate())
                .status(attendance.getStatus())
                .firstClockIn(attendance.getFirstClockIn())
                .lastClockOut(attendance.getLastClockOut())
                .workedMinutes(calculatedWorkedMinutes)  // 재계산된 값 사용
                .overtimeMinutes(attendance.getOvertimeMinutes())
                .daytimeOvertimeMinutes(attendance.getDaytimeOvertimeMinutes())
                .nightWorkMinutes(attendance.getNightWorkMinutes())
                .holidayWorkMinutes(attendance.getHolidayWorkMinutes())
                .totalBreakMinutes(attendance.getTotalBreakMinutes())
                .totalGoOutMinutes(attendance.getTotalGoOutMinutes())
                .isLate(attendance.getIsLate())
                .lateMinutes(attendance.getLateMinutes())
                .isEarlyLeave(attendance.getIsEarlyLeave())
                .earlyLeaveMinutes(attendance.getEarlyLeaveMinutes())
                .build();
    }

    /**
     * workedMinutes 재계산 로직
     * DB에 저장된 값과 무관하게 항상 최신 값으로 계산
     */
    private static Integer calculateWorkedMinutes(DailyAttendance attendance) {
        if (attendance.getFirstClockIn() == null || attendance.getLastClockOut() == null) {
            return 0;
        }

        // 총 근무시간 = (퇴근 - 출근) - 휴게시간 - 외출시간
        Duration duration = Duration.between(attendance.getFirstClockIn(), attendance.getLastClockOut());
        long grossMinutes = duration.toMinutes();

        int breakMinutes = attendance.getTotalBreakMinutes() != null ? attendance.getTotalBreakMinutes() : 0;
        int goOutMinutes = attendance.getTotalGoOutMinutes() != null ? attendance.getTotalGoOutMinutes() : 0;

        long netWorkMinutes = grossMinutes - breakMinutes - goOutMinutes;

        return (int) Math.max(0, netWorkMinutes);
    }
}
