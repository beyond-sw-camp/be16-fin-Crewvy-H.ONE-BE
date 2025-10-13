package com.crewvy.workforce_service.attendance.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.attendance.dto.request.PolicyCreateRequest;
import com.crewvy.workforce_service.attendance.dto.response.PolicyResponse;
import com.crewvy.workforce_service.attendance.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
