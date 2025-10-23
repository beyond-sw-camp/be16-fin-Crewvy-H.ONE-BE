package com.crewvy.workforce_service.attendance.validation;

import com.crewvy.common.exception.InvalidPolicyRuleException;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import org.springframework.stereotype.Component;

@Component("standard_workValidator") // Factory에서 찾을 수 있도록 Bean 이름 지정
public class StandardWorkValidator implements PolicyRuleValidator {
    @Override
    public void validate(PolicyRuleDetails details) {
        // 1. 필수 규칙 블록 존재 여부 검사
        if (details.getWorkTimeRule() == null) {
            throw new InvalidPolicyRuleException("표준 근무 정책에는 근무 시간 규칙(workTimeRule)이 필수입니다.");
        }
        if (details.getBreakRule() == null) {
            throw new InvalidPolicyRuleException("표준 근무 정책에는 휴게 시간 규칙(breakRule)이 필수입니다.");
        }

        // 2. 근무 시간 규칙 내부 필드 검사
        var workTimeRule = details.getWorkTimeRule();
        if (workTimeRule.getWorkStartTime() == null || workTimeRule.getWorkEndTime() == null) {
            throw new InvalidPolicyRuleException("근무 시간 규칙에 시작 및 종료 시각은 필수입니다.");
        }
        if (workTimeRule.getFixedWorkMinutes() == null || workTimeRule.getFixedWorkMinutes() <= 0) {
             throw new InvalidPolicyRuleException("고정 근무 시간(fixedWorkMinutes)은 필수이며 0보다 커야 합니다.");
        }

        // 3. 법적 휴게시간 준수 여부 검사 (근로기준법 제54조)
        var breakRule = details.getBreakRule();
        if (workTimeRule.getFixedWorkMinutes() >= 480) { // 8시간 이상 근무 시
            if (breakRule.getMandatoryBreakMinutes() == null || breakRule.getMandatoryBreakMinutes() < 60) {
                throw new InvalidPolicyRuleException("법규 위반: 8시간 이상 근무 시, 휴게 시간은 60분 이상이어야 합니다.");
            }
        } else if (workTimeRule.getFixedWorkMinutes() >= 240) { // 4시간 이상 근무 시
            if (breakRule.getMandatoryBreakMinutes() == null || breakRule.getMandatoryBreakMinutes() < 30) {
                throw new InvalidPolicyRuleException("법규 위반: 4시간 이상 근무 시, 휴게 시간은 30분 이상이어야 합니다.");
            }
        }
    }
}
