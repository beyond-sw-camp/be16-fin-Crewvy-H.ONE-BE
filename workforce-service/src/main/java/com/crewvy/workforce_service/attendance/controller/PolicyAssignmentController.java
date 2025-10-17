package com.crewvy.workforce_service.attendance.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.attendance.constant.PolicyScopeType;
import com.crewvy.workforce_service.attendance.dto.request.PolicyAssignmentRequest;
import com.crewvy.workforce_service.attendance.dto.response.PolicyAssignmentResponse;
import com.crewvy.workforce_service.attendance.service.PolicyAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/policy-assignments")
@RequiredArgsConstructor
public class PolicyAssignmentController {

    private final PolicyAssignmentService policyAssignmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> assignPolicies(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestBody @Valid PolicyAssignmentRequest request) {
        policyAssignmentService.assignPoliciesToTargets(memberPositionId, request);
        return new ResponseEntity<>(ApiResponse.success(null, "정책 할당 완료"), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PolicyAssignmentResponse>>> getPolicyAssignmentsByTarget(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestParam("targetId") UUID targetId,
            @RequestParam("targetType") PolicyScopeType targetType) {
        List<PolicyAssignmentResponse> response = policyAssignmentService.findPolicyAssignmentsByTarget(memberPositionId, targetId, targetType);
        return new ResponseEntity<>(ApiResponse.success(response, "정책 할당 목록 조회 완료"), HttpStatus.OK);
    }
}
