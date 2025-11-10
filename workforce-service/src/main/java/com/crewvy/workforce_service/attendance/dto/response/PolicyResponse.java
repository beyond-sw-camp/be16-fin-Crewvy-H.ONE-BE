package com.crewvy.workforce_service.attendance.dto.response;

import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import com.crewvy.workforce_service.attendance.entity.Policy;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
public class PolicyResponse {

    private final UUID policyId;
    private final PolicyTypeCode typeCode;
    private final String name;
    private final Boolean isPaid;
    private final Boolean isActive;
    private final boolean isBalanceDeductible;
    private final LocalDate effectiveFrom;
    private final LocalDate effectiveTo;
    private final Boolean autoApprove;
    private final PolicyRuleDetails ruleDetails;

    public PolicyResponse(Policy policy) {
        this.policyId = policy.getId();
        this.typeCode = policy.getPolicyTypeCode();
        this.name = policy.getName();
        this.isPaid = policy.getIsPaid();
        this.isActive = policy.getIsActive();
        this.isBalanceDeductible = policy.getPolicyTypeCode().isBalanceDeductible();
        this.effectiveFrom = policy.getEffectiveFrom();
        this.effectiveTo = policy.getEffectiveTo();
        this.autoApprove = policy.getAutoApprove();
        this.ruleDetails = policy.getRuleDetails();
    }
}
