package com.crewvy.workforce_service.attendance.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.attendance.dto.request.PolicyCreateRequest;
import com.crewvy.workforce_service.attendance.dto.request.PolicyUpdateRequest;
import com.crewvy.workforce_service.attendance.dto.response.PolicyResponse;
import com.crewvy.workforce_service.attendance.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    public ApiResponse<PolicyResponse> createPolicy(@RequestParam("companyId") UUID companyId,
                                                    @RequestBody @Valid PolicyCreateRequest request) {
        PolicyResponse response = policyService.createPolicy(companyId, request);
        return ApiResponse.success(response, "정책 생성 완료");
    }

    @GetMapping("/{policyId}")
    public ApiResponse<PolicyResponse> findPolicyById(@PathVariable UUID policyId) {
        PolicyResponse response = policyService.findPolicyById(policyId);
        return ApiResponse.success(response, "정책 조회");
    }

    @GetMapping
    public ApiResponse<Page<PolicyResponse>> findAllPolicies(
            @RequestParam("companyId") UUID companyId, Pageable pageable) {
        Page<PolicyResponse> response = policyService.findAllPoliciesByCompany(companyId, pageable);
        return ApiResponse.success(response, "회사 전체 정책 조회");
    }
    
    @PutMapping("/{policyId}")
        public ApiResponse<PolicyResponse> updatePolicy(
                @PathVariable UUID policyId,
                @RequestBody @Valid PolicyUpdateRequest request) {
            PolicyResponse response = policyService.updatePolicy(policyId, request);
            return ApiResponse.success(response, "정책 수정 완료");
        }
    
        @DeleteMapping("/{policyId}")
        public ApiResponse<Void> deletePolicy(@PathVariable UUID policyId) {
            policyService.deletePolicy(policyId);
            return ApiResponse.success(null, "정책 삭제 완료");
        }
    }