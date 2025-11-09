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
    private String joinDate;            // 입사일 (프론트 요청사항)
    private Integer year;               // 연도 (프론트 요청사항)
    private String policyTypeCode;
    private String policyTypeName;
    private Double totalGranted;
    private Double totalUsed;
    private Double remainingBalance;
    private Boolean isPaid;
    private Boolean isUsable;           // 사용 가능 여부
    private Double previousMonthAttendanceRate; // 전월 근속비율 (%)
}
