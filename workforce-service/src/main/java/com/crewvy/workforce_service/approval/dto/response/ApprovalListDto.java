package com.crewvy.workforce_service.approval.dto.response;

import com.crewvy.workforce_service.approval.constant.ApprovalState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ApprovalListDto {
    private UUID approvalId;
    private String title;
    private String documentName;
    private UUID requesterId; // 기안자 ID (추후 기안자 이름, 소속, 직급으로 수정)
    private LocalDateTime createAt; // 기안 일자
    private ApprovalState status; // 결재 상태
}
