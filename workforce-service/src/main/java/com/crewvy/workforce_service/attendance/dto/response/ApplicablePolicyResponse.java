package com.crewvy.workforce_service.attendance.dto.response;

import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.entity.Policy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicablePolicyResponse {

    private UUID policyId;
    private String policyName;
    private PolicyTypeCode policyTypeCode;
    private String policyTypeName;

    public static ApplicablePolicyResponse from(Policy policy) {
        return ApplicablePolicyResponse.builder()
                .policyId(policy.getId())
                .policyName(policy.getName())
                .policyTypeCode(policy.getPolicyType().getTypeCode())
                .policyTypeName(policy.getPolicyType().getTypeName())
                .build();
    }
}
