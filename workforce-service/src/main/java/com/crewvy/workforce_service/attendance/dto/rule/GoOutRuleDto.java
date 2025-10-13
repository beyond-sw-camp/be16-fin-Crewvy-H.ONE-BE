package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoOutRuleDto {
    // 외출 정책 타입: SIMPLE_RECORD (단순기록), APPROVAL_REQUIRED (결재필수)
    private String type;
    private Integer allowedMinutesWithoutApproval;
}
