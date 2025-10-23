package com.crewvy.workforce_service.attendance.validation;

import com.crewvy.common.exception.InvalidPolicyRuleException;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import org.springframework.stereotype.Component;

@Component("annual_leaveValidator")
public class AnnualLeaveValidator implements PolicyRuleValidator {
    @Override
    public void validate(PolicyRuleDetails details) {
        if (details.getLeaveRule() == null) {
            throw new InvalidPolicyRuleException("연차 정책에는 휴가 규칙(leaveRule)이 필수입니다.");
        }

        var leaveRule = details.getLeaveRule();
        if (leaveRule.getDefaultDays() == null) {
            throw new InvalidPolicyRuleException("연차 규칙에는 기본 부여 일수(defaultDays)가 필수입니다.");
        }

        // 근로기준법 제60조 (연차 유급휴가) - 1년간 80% 이상 출근 시 15일
        if (leaveRule.getDefaultDays() < 15) {
            throw new InvalidPolicyRuleException("법규 위반: 연차 기본 부여 일수는 최소 15일 이상이어야 합니다.");
        }
    }
}
