package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.attendance.constant.AttendanceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_attendance")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAttendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "daily_attendance_id", nullable = false)
    private UUID id;

    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private UUID memberId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "attendance_status", nullable = false)
    private AttendanceStatus status;

    @Column(name = "first_clock_in")
    private LocalDateTime firstClockIn;

    @Column(name = "last_clock_out")
    private LocalDateTime lastClockOut;

    @Column(name = "worked_minutes")
    private Integer workedMinutes;

    @Column(name = "overtime_minutes")
    private Integer overtimeMinutes;

    @Column(name = "total_break_minutes")
    private Integer totalBreakMinutes;

    @Builder.Default
    @Column(name = "total_go_out_minutes")
    private Integer totalGoOutMinutes = 0;

    @Builder.Default
    @Column(name = "is_late")
    private Boolean isLate = false;

    @Builder.Default
    @Column(name = "late_minutes")
    private Integer lateMinutes = 0;

    @Builder.Default
    @Column(name = "is_early_leave")
    private Boolean isEarlyLeave = false;

    @Builder.Default
    @Column(name = "early_leave_minutes")
    private Integer earlyLeaveMinutes = 0;

    @Builder.Default
    @Column(name = "daytime_overtime_minutes")
    private Integer daytimeOvertimeMinutes = 0;  // 주간 연장근무

    @Builder.Default
    @Column(name = "night_work_minutes")
    private Integer nightWorkMinutes = 0;        // 야간 근무 (22:00-06:00)

    @Builder.Default
    @Column(name = "holiday_work_minutes")
    private Integer holidayWorkMinutes = 0;      // 휴일 근무 (주말/CompanyHoliday)

    // 외출/휴게 상태 추적 (DB 저장 안함, 메모리상에서만 사용)
    @Transient
    private LocalDateTime currentGoOutStartTime;

    @Transient
    private LocalDateTime currentBreakStartTime;

    public void updateClockOut(LocalDateTime clockOutTime, Integer standardWorkMinutes) {
        this.lastClockOut = clockOutTime;
        if (this.firstClockIn != null) {
            Duration duration = Duration.between(this.firstClockIn, this.lastClockOut);
            long grossMinutes = duration.toMinutes();
            // 총 근무시간 = (퇴근-출근) - 총 휴게시간 - 총 외출시간
            long netWorkMinutes = grossMinutes - (this.totalBreakMinutes != null ? this.totalBreakMinutes : 0) - (this.totalGoOutMinutes != null ? this.totalGoOutMinutes : 0);
            this.workedMinutes = (int) Math.max(0, netWorkMinutes);

            // 초과근무 계산
            if (standardWorkMinutes != null && this.workedMinutes > standardWorkMinutes) {
                this.overtimeMinutes = this.workedMinutes - standardWorkMinutes;
            } else {
                this.overtimeMinutes = 0;
            }
        }
    }

    public void addGoOutMinutes(int minutes) {
        if (this.totalGoOutMinutes == null) {
            this.totalGoOutMinutes = 0;
        }
        this.totalGoOutMinutes += minutes;
    }

    public void addBreakMinutes(int minutes) {
        if (this.totalBreakMinutes == null) {
            this.totalBreakMinutes = 0;
        }
        this.totalBreakMinutes += minutes;
    }

    public void startGoOut(LocalDateTime goOutTime) {
        this.currentGoOutStartTime = goOutTime;
    }

    public void endGoOut(LocalDateTime comeBackTime) {
        if (this.currentGoOutStartTime == null) {
            throw new IllegalStateException("외출 시작 기록이 없습니다.");
        }
        Duration duration = Duration.between(this.currentGoOutStartTime, comeBackTime);
        int goOutMinutes = (int) duration.toMinutes();
        addGoOutMinutes(goOutMinutes);
        this.currentGoOutStartTime = null;
    }

    public boolean isOnGoOut() {
        return this.currentGoOutStartTime != null;
    }

    public void startBreak(LocalDateTime breakStartTime) {
        this.currentBreakStartTime = breakStartTime;
    }

    public void endBreak(LocalDateTime breakEndTime) {
        if (this.currentBreakStartTime == null) {
            throw new IllegalStateException("휴게 시작 기록이 없습니다.");
        }
        Duration duration = Duration.between(this.currentBreakStartTime, breakEndTime);
        int breakMinutes = (int) duration.toMinutes();
        if (this.totalBreakMinutes == null) {
            this.totalBreakMinutes = 0;
        }
        this.totalBreakMinutes += breakMinutes;
        this.currentBreakStartTime = null;
    }

    public boolean isOnBreak() {
        return this.currentBreakStartTime != null;
    }

    /**
     * 지각 여부 확인 및 설정
     * @param workStartTime 정규 출근 시각 (HH:mm 형식)
     * @param latenessGraceMinutes 지각 허용 시간(분)
     */
    public void checkAndSetLateness(String workStartTime, Integer latenessGraceMinutes) {
        if (this.firstClockIn == null || workStartTime == null) {
            return;
        }

        // workStartTime을 LocalTime으로 변환
        String[] timeParts = workStartTime.split(":");
        int standardHour = Integer.parseInt(timeParts[0]);
        int standardMinute = Integer.parseInt(timeParts[1]);

        LocalDateTime standardClockIn = this.attendanceDate.atTime(standardHour, standardMinute);

        // 지각 허용 시간 적용
        if (latenessGraceMinutes != null && latenessGraceMinutes > 0) {
            standardClockIn = standardClockIn.plusMinutes(latenessGraceMinutes);
        }

        // 출근 시각이 기준보다 늦으면 지각
        if (this.firstClockIn.isAfter(standardClockIn)) {
            this.isLate = true;
            Duration lateDuration = Duration.between(standardClockIn, this.firstClockIn);
            this.lateMinutes = (int) lateDuration.toMinutes();
        } else {
            this.isLate = false;
            this.lateMinutes = 0;
        }
    }

    /**
     * 조퇴 여부 확인 및 설정
     * @param workEndTime 정규 퇴근 시각 (HH:mm 형식)
     * @param earlyLeaveGraceMinutes 조퇴 허용 시간(분)
     */
    public void checkAndSetEarlyLeave(String workEndTime, Integer earlyLeaveGraceMinutes) {
        if (this.lastClockOut == null || workEndTime == null) {
            return;
        }

        // workEndTime을 LocalTime으로 변환
        String[] timeParts = workEndTime.split(":");
        int standardHour = Integer.parseInt(timeParts[0]);
        int standardMinute = Integer.parseInt(timeParts[1]);

        LocalDateTime standardClockOut = this.attendanceDate.atTime(standardHour, standardMinute);

        // 조퇴 허용 시간 적용 (퇴근 시각보다 이 시간만큼 일찍 퇴근해도 조퇴 아님)
        if (earlyLeaveGraceMinutes != null && earlyLeaveGraceMinutes > 0) {
            standardClockOut = standardClockOut.minusMinutes(earlyLeaveGraceMinutes);
        }

        // 퇴근 시각이 기준보다 이르면 조퇴
        if (this.lastClockOut.isBefore(standardClockOut)) {
            this.isEarlyLeave = true;
            Duration earlyDuration = Duration.between(this.lastClockOut, standardClockOut);
            this.earlyLeaveMinutes = (int) earlyDuration.toMinutes();
        } else {
            this.isEarlyLeave = false;
            this.earlyLeaveMinutes = 0;
        }
    }
}
