package com.crewvy.workforce_service.attendance.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.attendance.dto.request.PolicyCreateRequest;
import com.crewvy.workforce_service.attendance.dto.request.PolicyIdListRequest;
import com.crewvy.workforce_service.attendance.dto.request.PolicyUpdateRequest;
import com.crewvy.workforce_service.attendance.dto.response.PolicyResponse;
import com.crewvy.workforce_service.attendance.dto.response.PolicyTypeResponse;
import com.crewvy.workforce_service.attendance.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    public ResponseEntity<ApiResponse<PolicyResponse>> createPolicy(
            @RequestHeader("X-User-MemberPositionId") UUID memberpositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestHeader("X-User-OrganizationId") UUID organizationId,
            @RequestBody @Valid PolicyCreateRequest request) {
        PolicyResponse response = policyService.createPolicy(memberpositionId, companyId, organizationId, request);
        return new ResponseEntity<>(ApiResponse.success(response, "정책 생성 완료"), HttpStatus.CREATED);
    }

    @GetMapping("/{policyId}")
    public ResponseEntity<ApiResponse<PolicyResponse>> findPolicyById(
            @RequestHeader("X-User-MemberPositionId") UUID memberpositionId,
            @RequestParam("companyId") UUID companyId,
            @PathVariable UUID policyId) {
        PolicyResponse response = policyService.findPolicyById(memberpositionId, companyId, policyId);
        return new ResponseEntity<>(ApiResponse.success(response, "정책 조회"), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PolicyResponse>>> findAllPolicies(
            @RequestHeader("X-User-MemberPositionId") UUID memberpositionId,
            @RequestParam("companyId") UUID companyId,
            Pageable pageable) {
        Page<PolicyResponse> response = policyService.findAllPoliciesByCompany(memberpositionId, companyId, pageable);
        return new ResponseEntity<>(ApiResponse.success(response, "전체 정책 조회"), HttpStatus.OK);
    }

    @PutMapping("/{policyId}")
    public ResponseEntity<ApiResponse<PolicyResponse>> updatePolicy(
            @RequestHeader("X-User-MemberPositionId") UUID memberpositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID policyId,
            @RequestBody @Valid PolicyUpdateRequest request) {
        PolicyResponse response = policyService.updatePolicy(memberpositionId, companyId, policyId, request);
        return new ResponseEntity<>(ApiResponse.success(response, "정책 수정 완료"), HttpStatus.OK);
    }

    @DeleteMapping("/{policyId}")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(
            @RequestHeader("X-User-MemberPositionId") UUID memberpositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID policyId) {
        policyService.deletePolicy(memberpositionId, companyId, policyId);
        return new ResponseEntity<>(ApiResponse.success(null, "정책 삭제 완료"), HttpStatus.OK);
    }

    @PatchMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activatePolicies(
            @RequestHeader("X-User-MemberPositionId") UUID memberpositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestBody @Valid PolicyIdListRequest request) {
        policyService.activatePolicies(memberpositionId, companyId, request.getPolicyIds());
        return new ResponseEntity<>(ApiResponse.success(null, "정책 활성화 완료"), HttpStatus.OK);
    }

    @PatchMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivatePolicies(
            @RequestHeader("X-User-MemberPositionId") UUID memberpositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestBody @Valid PolicyIdListRequest request) {
        policyService.deactivatePolicies(memberpositionId, companyId, request.getPolicyIds());
        return new ResponseEntity<>(ApiResponse.success(null, "정책 비활성화 완료"), HttpStatus.OK);
    }

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<PolicyTypeResponse>>> findPolicyTypes(
            @RequestHeader("X-User-MemberPositionId") UUID memberpositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId) {
        List<PolicyTypeResponse> response = policyService.findPolicyTypesByCompany(memberpositionId, companyId);
        return new ResponseEntity<>(ApiResponse.success(response, "정책 유형 목록 조회"), HttpStatus.OK);
    }
}