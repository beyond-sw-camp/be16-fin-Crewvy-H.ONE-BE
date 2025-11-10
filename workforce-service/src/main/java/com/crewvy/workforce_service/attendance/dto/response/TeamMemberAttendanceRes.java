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

    private UUID dailyAttendanceId;   // 근태 기록 ID (수정 시 필요)
    private UUID memberId;
    private String name;              // 직원 이름
    private String department;        // 부서명
    private String title;             // 직책
    private String date;              // 날짜 (yyyy-MM-dd 형식)
    private String status;            // 근태 상태 (한글, 예: "정상근무", "연차")
    private Boolean isLate;           // 지각 여부
    private Boolean isEarlyLeave;     // 조퇴 여부
    private String clockInTime;       // 출근 시간 (HH:mm 형식)
    private String clockOutTime;      // 퇴근 시간 (HH:mm 형식)
    private String workHours;         // 근무 시간 (예: "8시간 30분")
    private String effectivePolicy;   // 적용 정책명

    // 승인된 Request 정보 (휴가, 출장 등)
    private String requestType;       // 신청 유형 (예: "연차", "반차", "출장")
    private String requestReason;     // 신청 사유
}
