package com.crewvy.workforce_service.attendance.validation;

import com.crewvy.common.exception.InvalidPolicyRuleException;
import com.crewvy.workforce_service.attendance.dto.rule.OvertimeRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 연장근무 정책 검증 로직.
 * 근로기준법 제53조, 제56조에 따른 법적 요구사항을 검증합니다.
 */
@Component("overtimeValidator")
public class OvertimeValidator implements PolicyRuleValidator {

    @Override
    public void validate(PolicyRuleDetails details) {
        // 1. 필수 규칙 블록 존재 여부 검사
        if (details.getOvertimeRule() == null) {
            throw new InvalidPolicyRuleException("연장근무 정책에는 연장근무 규칙(overtimeRule)이 필수입니다.");
        }

        OvertimeRuleDto overtimeRule = details.getOvertimeRule();

        // 2. 근로기준법 제53조 - 주간 최대 연장근무 한도 12시간(720분) 검증
        if (overtimeRule.getMaxWeeklyOvertimeMinutes() != null && overtimeRule.getMaxWeeklyOvertimeMinutes() > 720) {
            throw new InvalidPolicyRuleException(
                "법규 위반: 주간 최대 연장근무 한도는 12시간(720분)을 초과할 수 없습니다. (근로기준법 제53조)"
            );
        }

        // 3. 근로기준법 제56조 - 연장근무 가산임금률 1.5배 이상 검증
        if (overtimeRule.getOvertimeRate() != null && overtimeRule.getOvertimeRate().compareTo(new BigDecimal("1.5")) < 0) {
            throw new InvalidPolicyRuleException(
                "법규 위반: 연장근무 가산임금률은 최소 1.5배 이상이어야 합니다. (근로기준법 제56조)"
            );
        }
    }
}
