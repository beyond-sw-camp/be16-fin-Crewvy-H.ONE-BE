package com.crewvy.workforce_service.attendance.dto.request;

import com.crewvy.workforce_service.attendance.constant.PolicyScopeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
        @NotNull(message = "정책 ID는 필수입니다.")
        private UUID policyId;

        @NotNull(message = "할당 대상 ID는 필수입니다.")
        private UUID targetId;

        @NotNull(message = "할당 범위는 필수입니다.")
        private PolicyScopeType scopeType;
    }
}
