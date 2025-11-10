package com.crewvy.workforce_service.attendance.dto.response;

import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import lombok.Getter;

@Getter
public class PolicyTypeResponse {
    private final String typeCode;
    private final String typeName;
    private final boolean isBalanceDeductible;

    public PolicyTypeResponse(PolicyTypeCode policyTypeCode) {
        this.typeCode = policyTypeCode.getCodeValue();
        this.typeName = policyTypeCode.getCodeName();
        this.isBalanceDeductible = policyTypeCode.isBalanceDeductible();
    }
}
