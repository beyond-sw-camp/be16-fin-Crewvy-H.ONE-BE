package com.crewvy.workforce_service.attendance.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class PolicyIdListRequest {

    @NotEmpty(message = "정책 ID 목록이 비어있을 수 없습니다.")
    private List<UUID> policyIds;
}
