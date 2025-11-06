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
    private String memberName;          // 직원명
    private String organizationName;    // 조직명
    private String titleName;           // 직책명
    private String policyTypeCode;
    private String policyTypeName;
    private Double totalGranted;
    private Double totalUsed;
    private Double remainingBalance;
    private Boolean isPaid;
    private Boolean isUsable;           // 사용 가능 여부
}
