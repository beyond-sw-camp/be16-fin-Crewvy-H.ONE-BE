package com.crewvy.workforce_service.attendance.dto.response;

import com.crewvy.workforce_service.attendance.entity.PolicyType;
import lombok.Getter;

import java.util.UUID;

@Getter
public class PolicyTypeResponse {
    private final UUID policyTypeId;
    private final String typeCode;
    private final String typeName;
    private final boolean isBalanceDeductible;

    public PolicyTypeResponse(PolicyType policyType) {
        this.policyTypeId = policyType.getId();
        this.typeCode = policyType.getTypeCode().getCodeValue(); // .getCodeValue() 추가
        this.typeName = policyType.getTypeName();
        this.isBalanceDeductible = policyType.isBalanceDeductible();
    }
}
