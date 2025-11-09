package com.crewvy.workforce_service.attendance.event;

import com.crewvy.workforce_service.approval.constant.ApprovalState;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 근태 Request가 결재 승인/반려되었을 때 발행되는 이벤트
 * ApprovalService -> AttendanceService 간 결합도를 낮추기 위해 사용
 */
@Getter
@AllArgsConstructor
public class AttendanceRequestApprovedEvent {

    private final UUID requestId;           // 근태 신청 ID
    private final UUID approvalId;          // 결재 문서 ID
    private final ApprovalState approvalState; // 최종 결재 상태 (APPROVED, REJECTED)
    private final LocalDateTime completedAt;   // 결재 완료 시각
}
