package com.crewvy.workforce_service.attendance.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class PolicyAssignmentIdListRequest {
    @NotEmpty(message = "해지할 정책 할당 ID 목록은 비어 있을 수 없습니다.")
    private List<UUID> assignmentIds;
}
