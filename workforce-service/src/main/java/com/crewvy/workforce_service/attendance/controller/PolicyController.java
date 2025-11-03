package com.crewvy.workforce_service.attendance.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.attendance.dto.request.PolicyCreateRequest;
import com.crewvy.workforce_service.attendance.dto.request.PolicyIdListRequest;
import com.crewvy.workforce_service.attendance.dto.request.PolicyUpdateRequest;
import com.crewvy.workforce_service.attendance.dto.response.ApplicablePolicyResponse;
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
            @RequestBody @Valid PolicyCreateRequest createRequest) {
        PolicyResponse response = policyService.createPolicy(memberpositionId, companyId, organizationId, createRequest);
        return new ResponseEntity<>(ApiResponse.success(response, "정책 생성 완료"), HttpStatus.CREATED);
    }

    @GetMapping("/applicable-to-me")
    public ResponseEntity<ApiResponse<List<ApplicablePolicyResponse>>> getApplicablePoliciesForMe(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestHeader("X-User-OrganizationId") UUID organizationId) {
        List<ApplicablePolicyResponse> response = policyService.findApplicablePoliciesForMember(memberId, memberPositionId, companyId, organizationId);
        return new ResponseEntity<>(ApiResponse.success(response, "신청 가능한 정책 목록 조회 완료"), HttpStatus.OK);
    }

    @GetMapping("/{policyId}")
    public ResponseEntity<ApiResponse<PolicyResponse>> findPolicyById(
            @RequestHeader("X-User-MemberPositionId") UUID memberpositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID policyId) {
        PolicyResponse response = policyService.findPolicyById(memberpositionId, companyId, policyId);
        return new ResponseEntity<>(ApiResponse.success(response, "정책 조회"), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PolicyResponse>>> findAllPolicies(
            @RequestHeader("X-User-MemberPositionId") UUID memberpositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            Pageable pageable) {
        Page<PolicyResponse> response = policyService.findAllPoliciesByCompany(memberpositionId, companyId, pageable);
        return new ResponseEntity<>(ApiResponse.success(response, "전체 정책 조회"), HttpStatus.OK);
    }

    @PutMapping("/{policyId}")
    public ResponseEntity<ApiResponse<PolicyResponse>> updatePolicy(
            @RequestHeader("X-User-MemberPositionId") UUID memberpositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID policyId,
            @RequestBody @Valid PolicyUpdateRequest updateRequest) {
        PolicyResponse response = policyService.updatePolicy(memberpositionId, companyId, policyId, updateRequest);
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
            @RequestBody @Valid PolicyIdListRequest idListRequest) {
        policyService.activatePolicies(memberpositionId, companyId, idListRequest.getPolicyIds());
        return new ResponseEntity<>(ApiResponse.success(null, "정책 활성화 완료"), HttpStatus.OK);
    }

    @PatchMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivatePolicies(
            @RequestHeader("X-User-MemberPositionId") UUID memberpositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestBody @Valid PolicyIdListRequest idListRequest) {
        policyService.deactivatePolicies(memberpositionId, companyId, idListRequest.getPolicyIds());
        return new ResponseEntity<>(ApiResponse.success(null, "정책 비활성화 완료"), HttpStatus.OK);
    }

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<PolicyTypeResponse>>> findPolicyTypes(
            @RequestHeader("X-User-MemberPositionId") UUID memberpositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId) {
        List<PolicyTypeResponse> response = policyService.findPolicyTypesByCompany(memberpositionId, companyId);
        return new ResponseEntity<>(ApiResponse.success(response, "정책 유형 목록 조회"), HttpStatus.OK);
    }

    @GetMapping("/my-effective-policy")
    public ResponseEntity<ApiResponse<PolicyResponse>> getMyEffectivePolicy(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestHeader("X-User-OrganizationId") UUID organizationId) {
        PolicyResponse response = policyService.findMyEffectivePolicy(memberId, companyId, organizationId);
        return new ResponseEntity<>(ApiResponse.success(response, "나의 유효 정책 조회 완료"), HttpStatus.OK);
    }

}