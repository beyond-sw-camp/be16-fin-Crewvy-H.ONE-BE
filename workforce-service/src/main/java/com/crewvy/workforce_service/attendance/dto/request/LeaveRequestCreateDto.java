package com.crewvy.workforce_service.attendance.dto.request;

import com.crewvy.workforce_service.attendance.constant.RequestUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class LeaveRequestCreateDto {

    // 추가근무 신청 시에는 policyId가 null이어도 됨 (자동 분류)
    private UUID policyId;           // 적용할 정책 ID

    // 추가근무 신청 시에는 requestUnit이 null이어도 됨 (자동으로 TIME_OFF 또는 DAY 설정)
    private RequestUnit requestUnit; // 신청 단위 (DAY, HALF_DAY_AM, HALF_DAY_PM, TIME_OFF)

    // 일차/반차 신청 시 사용
    private LocalDate startAt;       // 시작일
    private LocalDate endAt;         // 종료일

    // 시차 신청 시 사용
    private LocalDateTime startDateTime; // 시작 시각
    private LocalDateTime endDateTime;   // 종료 시각

    // 추가근무 신청 시 사용 (방식 A: 기간 + 1일 연장시간)
    private String dailyOvertimeHours;   // 1일 연장시간 (HH:mm 형식, 예: "02:00")

    @NotBlank(message = "사유는 필수값입니다.")
    private String reason;           // 사유

    private String requesterComment; // 신청자 코멘트 (선택)

    private String workLocation;     // 출장지 (출장 신청 시 사용)

    private UUID documentId;         // 결재 문서 ID (선택 - 결재가 필요한 경우)
}
