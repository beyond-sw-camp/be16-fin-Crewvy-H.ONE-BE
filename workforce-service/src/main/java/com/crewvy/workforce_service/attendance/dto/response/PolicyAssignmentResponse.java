package com.crewvy.workforce_service.attendance.dto.response;

import com.crewvy.workforce_service.attendance.constant.PolicyScopeType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class PolicyAssignmentResponse {
    private UUID policyAssignmentId;
    private UUID policyId;
    private UUID targetId;
    private PolicyScopeType scopeType;
    private boolean isActive;
    private UUID assignedBy;
    private LocalDateTime assignedAt;
    private LocalDateTime revokedAt;
}
