package com.crewvy.workforce_service.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedPolicyRes {
    private UUID policyId;
    private String name;
    private String typeCode;
    private String typeName;
    private Boolean isActive;
    private List<String> allowedRequestUnits; // 허용된 신청 단위 (예: ["DAY", "HALF_DAY_AM", "HALF_DAY_PM"])
}
