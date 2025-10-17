package com.crewvy.workforce_service.approval.dto.response;

import com.crewvy.workforce_service.approval.constant.LineStatus;
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
public class ApprovalStepDto {
    private UUID approverId;
    private String approverName;
    private String approverPosition;
    private String approverOrganization;
    private LineStatus status;
    private int index;
    private LocalDateTime approveAt;
    private String comment;
}
