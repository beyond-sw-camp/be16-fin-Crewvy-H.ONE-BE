package com.crewvy.workforce_service.attendance.dto.request;

import com.crewvy.workforce_service.attendance.constant.PolicyScopeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class PolicyAssignmentRequest {

    @NotEmpty(message = "정책 할당 목록은 비어 있을 수 없습니다.")
    private List<@Valid SingleAssignmentRequest> assignments;

    @Getter
    @NoArgsConstructor
    public static class SingleAssignmentRequest {
        private UUID policyId;
        private UUID targetId;
        private PolicyScopeType scopeType;
    }
}
