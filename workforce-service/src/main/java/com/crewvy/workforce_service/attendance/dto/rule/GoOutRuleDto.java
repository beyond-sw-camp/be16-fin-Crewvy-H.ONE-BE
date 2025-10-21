package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Getter;
import lombok.Setter;

/**
 * 외출 정책 규칙
 */
@Getter
@Setter
public class GoOutRuleDto {
    // 외출 정책 타입: SIMPLE_RECORD (단순기록), APPROVAL_REQUIRED (결재필수)
    private String type;

    // 결재 없이 허용되는 외출 시간 (분) - SIMPLE_RECORD 타입일 때 사용
    private Integer allowedMinutesWithoutApproval;

    // 일일 최대 외출 시간 (분) - null이면 제한 없음
    private Integer maxDailyGoOutMinutes;

    // 1회 최대 외출 시간 (분) - null이면 제한 없음
    private Integer maxSingleGoOutMinutes;
}
