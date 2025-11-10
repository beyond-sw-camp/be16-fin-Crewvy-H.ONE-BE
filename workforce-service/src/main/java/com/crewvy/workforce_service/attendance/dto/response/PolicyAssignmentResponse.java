package com.crewvy.workforce_service.attendance.dto.response;

import com.crewvy.workforce_service.attendance.constant.PolicyScopeType;
import com.crewvy.workforce_service.attendance.entity.PolicyAssignment;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter // 클래스 레벨로 이동
@AllArgsConstructor
public class PolicyAssignmentResponse {
    private UUID policyAssignmentId;
    private UUID policyId;
    private String policyName;
    private UUID targetId;
    private PolicyScopeType scopeType;
    private String targetName;
    private String targetAffiliation; // 소속 필드 추가
    @JsonProperty("isActive")
    private boolean isActive;
    private UUID assignedBy;
    private LocalDateTime assignedAt;
    private LocalDateTime revokedAt;

    public PolicyAssignmentResponse(PolicyAssignment entity) {
        this.policyAssignmentId = entity.getId();
        this.policyId = entity.getPolicy().getId();
        this.policyName = entity.getPolicy().getName();
        this.targetId = entity.getTargetId();
        this.scopeType = entity.getScopeType();
        this.isActive = entity.isActive();
        this.assignedBy = entity.getAssignedBy();
        this.assignedAt = entity.getAssignedAt();
        this.revokedAt = entity.getRevokedAt();
        this.targetName = null;
        this.targetAffiliation = null; // 필드 초기화
    }
}
