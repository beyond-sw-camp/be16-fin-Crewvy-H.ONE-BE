package com.crewvy.workforce_service.attendance.validation;

import com.crewvy.common.exception.InvalidPolicyRuleException;
import com.crewvy.workforce_service.attendance.dto.rule.LeaveRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import org.springframework.stereotype.Component;

/**
 * 출산전후휴가 정책 검증 로직.
 * 근로기준법 제74조에 따른 법적 요구사항을 검증합니다.
 */
@Component("maternity_leaveValidator")
public class MaternityLeaveValidator implements PolicyRuleValidator {

    @Override
    public void validate(PolicyRuleDetails details) {
        // 1. 필수 규칙 블록 존재 여부 검사
        if (details.getLeaveRule() == null) {
            throw new InvalidPolicyRuleException("출산전후휴가 정책에는 휴가 규칙(leaveRule)이 필수입니다.");
        }

        LeaveRuleDto leaveRule = details.getLeaveRule();

        // 2. 기본 부여 일수 필수 검사
        if (leaveRule.getDefaultDays() == null) {
            throw new InvalidPolicyRuleException("출산전후휴가 규칙에는 기본 부여 일수(defaultDays)가 필수입니다.");
        }

        // 3. 근로기준법 제74조 - 출산전후휴가는 90일 이상이어야 함
        // (다태아의 경우 120일이나, 시스템에서는 최소 90일로 제한)
        if (leaveRule.getDefaultDays() < 90) {
            throw new InvalidPolicyRuleException(
                "법규 위반: 출산전후휴가는 최소 90일 이상이어야 합니다. (근로기준법 제74조)"
            );
        }
    }
}
