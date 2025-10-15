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
            @RequestParam("companyId") UUID companyId,
            @RequestBody @Valid PolicyCreateRequest request) {
        PolicyResponse response = policyService.createPolicy(companyId, request);
        return new ResponseEntity<>(ApiResponse.success(response, "정책 생성 완료"), HttpStatus.CREATED);
    }

    @GetMapping("/{policyId}")
    public ResponseEntity<ApiResponse<PolicyResponse>> findPolicyById(@PathVariable UUID policyId) {
        PolicyResponse response = policyService.findPolicyById(policyId);
        return new ResponseEntity<>(ApiResponse.success(response, "정책 조회"), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PolicyResponse>>> findAllPolicies(
            @RequestParam("companyId") UUID companyId, Pageable pageable) {
        Page<PolicyResponse> response = policyService.findAllPoliciesByCompany(companyId, pageable);
        return new ResponseEntity<>(ApiResponse.success(response, "전체 정책 조회"), HttpStatus.OK);
    }

    @PutMapping("/{policyId}")
    public ResponseEntity<ApiResponse<PolicyResponse>> updatePolicy(@PathVariable UUID policyId,
                                                    @RequestBody @Valid PolicyUpdateRequest request) {
        PolicyResponse response = policyService.updatePolicy(policyId, request);
        return new ResponseEntity<>(ApiResponse.success(response, "정책 수정 완료"), HttpStatus.OK);
    }

    @DeleteMapping("/{policyId}")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(@PathVariable UUID policyId) {
        policyService.deletePolicy(policyId);
        return new ResponseEntity<>(ApiResponse.success(null, "정책 삭제 완료"), HttpStatus.OK);
    }

    @PatchMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activatePolicies(@RequestBody @Valid PolicyIdListRequest request) {
        policyService.activatePolicies(request.getPolicyIds());
        return new ResponseEntity<>(ApiResponse.success(null, "정책 활성화 완료"), HttpStatus.OK);
    }

    @PatchMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivatePolicies(@RequestBody @Valid PolicyIdListRequest request) {
        policyService.deactivatePolicies(request.getPolicyIds());
        return new ResponseEntity<>(ApiResponse.success(null, "정책 비활성화 완료"), HttpStatus.OK);
    }

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<PolicyTypeResponse>>> findPolicyTypes(@RequestHeader("X-Company-Id") UUID companyId) {
        List<PolicyTypeResponse> response = policyService.findPolicyTypesByCompany(companyId);
        return new ResponseEntity<>(ApiResponse.success(response, "정책 유형 목록 조회"), HttpStatus.OK);
    }
}