package com.crewvy.workforce_service.attendance.dto.response;

import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.constant.RequestUnit;
import com.crewvy.workforce_service.attendance.entity.Request;
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
public class LeaveRequestResponse {

    private UUID requestId;

    private UUID policyId;

    private String policyName;          // 정책 이름

    private String policyTypeName;      // 정책 타입 이름 (연차, 출산전후휴가 등)

    private UUID memberId;

    private UUID documentId;            // 결재 문서 ID

    private RequestUnit requestUnit;

    private LocalDate startAt;

    private LocalDate endAt;

    private Double deductionDays;       // 차감 일수

    private String reason;

    private RequestStatus status;

    private String requesterComment;

    private String workLocation;        // 출장지

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static LeaveRequestResponse from(Request request) {
        return LeaveRequestResponse.builder()
                .requestId(request.getId())
                .policyId(request.getPolicy().getId())
                .policyName(request.getPolicy().getName())
                .policyTypeName(request.getPolicy().getPolicyType().getTypeName())
                .memberId(request.getMemberId())
                .documentId(request.getDocumentId())
                .requestUnit(request.getRequestUnit())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .deductionDays(request.getDeductionDays())
                .reason(request.getReason())
                .status(request.getStatus())
                .requesterComment(request.getRequesterComment())
                .workLocation(request.getWorkLocation())
                .completedAt(request.getCompletedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
