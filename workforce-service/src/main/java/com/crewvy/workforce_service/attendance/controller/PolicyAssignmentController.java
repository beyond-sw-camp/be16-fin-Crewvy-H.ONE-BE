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
    public ResponseEntity<ApiResponse<List<PolicyAssignmentResponse>>> createAssignments(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestBody @Valid PolicyAssignmentRequest request) {
        
        List<PolicyAssignmentResponse> response = policyAssignmentService.createAssignments(memberPositionId, request);
        return new ResponseEntity<>(ApiResponse.success(response, "정책이 성공적으로 할당되었습니다."), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PolicyAssignmentResponse>>> findAssignments(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId) {
        
        List<PolicyAssignmentResponse> response = policyAssignmentService.findAssignments(memberId, memberPositionId, companyId);
        return new ResponseEntity<>(ApiResponse.success(response, "정책 할당 목록 조회 완료"), HttpStatus.OK);
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @PathVariable UUID assignmentId) {
        
        policyAssignmentService.deleteAssignment(memberPositionId, assignmentId);
        return new ResponseEntity<>(ApiResponse.success(null, "정책 할당이 삭제되었습니다."), HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PolicyAssignmentResponse>>> findAssignmentsByTarget(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestParam("targetId") UUID targetId,
            @RequestParam(value = "scopeType", required = false) PolicyScopeType scopeType) {
        
        List<PolicyAssignmentResponse> response = policyAssignmentService.findPolicyAssignmentsByTarget(memberPositionId, targetId, scopeType);
        return new ResponseEntity<>(ApiResponse.success(response, "대상별 정책 할당 목록 조회 완료"), HttpStatus.OK);
    }

    @PatchMapping("/{assignmentId}/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeAssignment(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @PathVariable UUID assignmentId) {
        
        policyAssignmentService.revokePolicyAssignment(memberPositionId, assignmentId);
        return new ResponseEntity<>(ApiResponse.success(null, "정책 할당이 해지되었습니다."), HttpStatus.OK);
    }

    @PatchMapping("/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeAssignments(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestBody @Valid com.crewvy.workforce_service.attendance.dto.request.PolicyAssignmentIdListRequest request) {
        
        policyAssignmentService.revokeAssignments(memberPositionId, request.getAssignmentIds());
        return new ResponseEntity<>(ApiResponse.success(null, "선택된 정책 할당이 모두 해지되었습니다."), HttpStatus.OK);
    }
}
