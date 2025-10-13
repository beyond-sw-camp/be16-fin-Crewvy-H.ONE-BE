package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.entity.BaseEntity;
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
    private UUID dailyAttendanceId;

    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private UUID memberId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "first_clock_in", nullable = false)
    private LocalDateTime firstClockIn;

    @Column(name = "last_clock_out")
    private LocalDateTime lastClockOut;

    @Column(name = "worked_minutes")
    private Integer workedMinutes;

    @Column(name = "overtime_minutes")
    private Integer overtimeMinutes;

    @Column(name = "total_break_minutes")
    private Integer totalBreakMinutes;

    @Column(name = "total_go_out_minutes")
    private Integer totalGoOutMinutes = 0;

    public void updateClockOut(LocalDateTime clockOutTime) {
        this.lastClockOut = clockOutTime;
        if (this.firstClockIn != null) {
            Duration duration = Duration.between(this.firstClockIn, this.lastClockOut);
            long grossMinutes = duration.toMinutes();
            // 총 근무시간 = (퇴근-출근) - 총 휴게시간 - 총 외출시간
            long netWorkMinutes = grossMinutes - (this.totalBreakMinutes != null ? this.totalBreakMinutes : 0) - (this.totalGoOutMinutes != null ? this.totalGoOutMinutes : 0);
            this.workedMinutes = (int) Math.max(0, netWorkMinutes);

            // TODO: 초과근무 계산 로직 (이제 netWorkMinutes를 기준으로 계산)
        }
    }

    public void addGoOutMinutes(int minutes) {
        if (this.totalGoOutMinutes == null) {
            this.totalGoOutMinutes = 0;
        }
        this.totalGoOutMinutes += minutes;
    }
}
