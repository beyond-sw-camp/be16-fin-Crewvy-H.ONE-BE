package com.crewvy.workforce_service.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 근태 현황 응답 DTO
 * (통합 근태 현황 화면에서 사용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberAttendanceRes {

    private UUID memberId;
    private String name;              // 직원 이름
    private String department;        // 부서명
    private String title;             // 직책
    private String date;              // 날짜 (yyyy-MM-dd 형식)
    private String statusCode;        // 상태 코드 (영문, 예: NORMAL_WORK, ANNUAL_LEAVE)
    private Boolean isLate;           // 지각 여부
    private String clockInTime;       // 출근 시간 (HH:mm 형식)
    private String clockOutTime;      // 퇴근 시간 (HH:mm 형식)
    private String workHours;         // 근무 시간 (예: "8시간 30분")
    private String effectivePolicy;   // 적용 정책명
}
