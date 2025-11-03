package com.crewvy.workforce_service.attendance.validation;

import com.crewvy.common.exception.InvalidPolicyRuleException;
import com.crewvy.workforce_service.attendance.dto.rule.OvertimeRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 야간근무 정책 검증 로직.
 * 근로기준법 제56조에 따른 법적 요구사항을 검증합니다.
 */
@Component("night_workValidator")
public class NightWorkValidator implements PolicyRuleValidator {

    @Override
    public void validate(PolicyRuleDetails details) {
        // 1. 필수 규칙 블록 존재 여부 검사
        if (details.getOvertimeRule() == null) {
            throw new InvalidPolicyRuleException("야간근무 정책에는 연장근무 규칙(overtimeRule)이 필수입니다.");
        }

        OvertimeRuleDto overtimeRule = details.getOvertimeRule();

        // 2. 야간근무 허용 여부 검사
        if (!overtimeRule.isAllowNightWork()) {
            throw new InvalidPolicyRuleException("야간근무 정책에서는 야간근무 허용(allowNightWork)이 true여야 합니다.");
        }

        // 3. 근로기준법 제56조 - 야간근무 가산임금률 1.5배 이상 검증
        if (overtimeRule.getNightWorkRate() != null && overtimeRule.getNightWorkRate().compareTo(new BigDecimal("1.5")) < 0) {
            throw new InvalidPolicyRuleException(
                "법규 위반: 야간근무 가산임금률은 최소 1.5배 이상이어야 합니다. (근로기준법 제56조)"
            );
        }
    }
}
