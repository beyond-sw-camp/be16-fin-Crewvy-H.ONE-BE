package com.crewvy.workforce_service.attendance.validation;

import com.crewvy.common.exception.InvalidPolicyRuleException;
import com.crewvy.workforce_service.attendance.dto.rule.LeaveRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import org.springframework.stereotype.Component;

/**
 * 생리휴가 정책 검증 로직.
 * 근로기준법 제73조에 따른 법적 요구사항을 검증합니다.
 */
@Component("menstrual_leaveValidator")
public class MenstrualLeaveValidator implements PolicyRuleValidator {

    @Override
    public void validate(PolicyRuleDetails details) {
        // 1. 필수 규칙 블록 존재 여부 검사
        if (details.getLeaveRule() == null) {
            throw new InvalidPolicyRuleException("생리휴가 정책에는 휴가 규칙(leaveRule)이 필수입니다.");
        }

        LeaveRuleDto leaveRule = details.getLeaveRule();

        // 2. 기본 부여 일수 필수 검사
        if (leaveRule.getDefaultDays() == null) {
            throw new InvalidPolicyRuleException("생리휴가 규칙에는 기본 부여 일수(defaultDays)가 필수입니다.");
        }

        // 3. 근로기준법 제73조 - 생리휴가는 월 1일 이상이어야 함
        if (leaveRule.getDefaultDays() < 1) {
            throw new InvalidPolicyRuleException(
                "법규 위반: 생리휴가는 최소 월 1일 이상이어야 합니다. (근로기준법 제73조)"
            );
        }

        // 4. 실무적 상한선 (월 3일 정도가 적절)
        if (leaveRule.getDefaultDays() > 3) {
            throw new InvalidPolicyRuleException("생리휴가는 월 3일을 초과하여 설정할 수 없습니다.");
        }

        // 5. 주기별 제한 검증 (월간 제한)
        if (leaveRule.getLimitPeriod() != null) {
            if (!leaveRule.getLimitPeriod().equals("MONTHLY")) {
                throw new InvalidPolicyRuleException("생리휴가의 제한 주기(limitPeriod)는 'MONTHLY'여야 합니다.");
            }
        }

        if (leaveRule.getMaxDaysPerPeriod() != null) {
            if (leaveRule.getMaxDaysPerPeriod() < 1) {
                throw new InvalidPolicyRuleException(
                    "법규 위반: 월간 최대 사용 일수(maxDaysPerPeriod)는 최소 1일 이상이어야 합니다. (근로기준법 제73조)"
                );
            }
            if (leaveRule.getMaxDaysPerPeriod() > 3) {
                throw new InvalidPolicyRuleException("월간 최대 사용 일수는 3일을 초과할 수 없습니다.");
            }
        }
    }
}
