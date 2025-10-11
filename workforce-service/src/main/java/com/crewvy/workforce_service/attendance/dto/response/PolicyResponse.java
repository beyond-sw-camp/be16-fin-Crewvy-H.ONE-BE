package com.crewvy.workforce_service.attendance.dto.response;

import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import com.crewvy.workforce_service.attendance.entity.Policy;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
public class PolicyResponse {

    private UUID policyId;
    private UUID policyTypeId;
    private String name;
    private Boolean isPaid;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private PolicyRuleDetails ruleDetails;

    public PolicyResponse(Policy policy) {
        this.policyId = policy.getPolicyId();
        this.policyTypeId = policy.getPolicyType().getPolicyTypeId();
        this.name = policy.getName();
        this.isPaid = policy.getIsPaid();
        this.effectiveFrom = policy.getEffectiveFrom();
        this.effectiveTo = policy.getEffectiveTo();
        this.ruleDetails = policy.getRuleDetails();
    }
}
