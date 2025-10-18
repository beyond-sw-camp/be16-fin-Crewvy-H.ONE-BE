package com.crewvy.workforce_service.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberBalanceSummaryRes {
    private UUID memberId;
    private String policyTypeCode;
    private String policyTypeName;
    private Double remainingBalance;
}
