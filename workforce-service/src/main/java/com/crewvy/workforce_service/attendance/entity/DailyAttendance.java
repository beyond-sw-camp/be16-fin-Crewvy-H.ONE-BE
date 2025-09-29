package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.entity.BaseEntity;
import jakarta.persistence.*;
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
public class DailyAttendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    /**
     * 계산된 근무 시간(분)
     * - AttendanceLog 기반으로 산출
     * - 빠른 조회를 위해 캐싱해둔 값
     * - 정책 변경 시 재계산 필요 가능
     */
    @Column(name = "worked_minutes", nullable = false)
    private Integer workedMinutes;

    /**
     * 계산된 연장근무 시간(분)
     * - 회사 정책 (예: 9시간 이상 근무) 기준으로 산출
     * - 파생값 (캐싱)
     */
    @Column(name = "overtime_minutes", nullable = false)
    private Integer overtimeMinutes;

    /**
     * 계산된 총 휴게 시간(분)
     * - AttendanceLog의 휴게 이벤트 기반
     * - 파생값 (캐싱)
     */
    @Column(name = "total_break_minutes", nullable = false)
    private Integer totalBreakMinutes;
}
