package com.crewvy.workforce_service.attendance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 출장 신청 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripRequestCreateDto {

    @NotNull(message = "정책 ID는 필수값입니다.")
    private UUID policyId;           // 적용할 출장 정책 ID

    @NotNull(message = "시작일은 필수값입니다.")
    private LocalDate startAt;       // 출장 시작일

    @NotNull(message = "종료일은 필수값입니다.")
    private LocalDate endAt;         // 출장 종료일

    @NotBlank(message = "출장지는 필수값입니다.")
    private String workLocation;     // 출장지

    @NotBlank(message = "사유는 필수값입니다.")
    private String reason;           // 출장 사유

    private String requesterComment; // 신청자 코멘트 (선택)

    private UUID documentId;         // 결재 문서 ID (선택 - 결재가 필요한 경우)
}
