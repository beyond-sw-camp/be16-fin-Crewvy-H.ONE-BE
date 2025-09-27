package com.crewvy.workforce_service.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_attendance")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAttendance {

    @Id
    @Column(name = "daily_attendance_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID dailyAttendanceId;

    @Column(name = "member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "first_clock_in", nullable = false)
    private LocalDateTime firstClockIn;

    @Column(name = "last_clock_out", nullable = false)
    private LocalDateTime lastClockOut;

    @Column(name = "worked_minutes", nullable = false)
    private Integer workedMinutes;

    @Column(name = "overtime_minutes", nullable = false)
    private Integer overtimeMinutes;

    @Column(name = "total_break_minutes", nullable = false)
    private Integer totalBreakMinutes;
}
