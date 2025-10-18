package com.crewvy.workforce_service.attendance.dto.response;

import lombok.Getter;

import java.util.List;

@Getter

public class PolicyAssignmentListResponse {
    private List<PolicyAssignmentResponse> assignedPolicies;
}
