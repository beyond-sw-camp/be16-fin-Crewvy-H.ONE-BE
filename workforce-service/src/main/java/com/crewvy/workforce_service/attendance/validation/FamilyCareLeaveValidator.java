package com.crewvy.workforce_service.attendance.validation;

import com.crewvy.common.exception.InvalidPolicyRuleException;
import com.crewvy.workforce_service.attendance.dto.rule.LeaveRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import org.springframework.stereotype.Component;

/**
 * 가족돌봄휴가 정책 검증 로직.
 * 남녀고용평등법 제22조의2에 따른 법적 요구사항을 검증합니다.
 */
@Component("family_care_leaveValidator")
public class FamilyCareLeaveValidator implements PolicyRuleValidator {

    @Override
    public void validate(PolicyRuleDetails details) {
        // 1. 필수 규칙 블록 존재 여부 검사
        if (details.getLeaveRule() == null) {
            throw new InvalidPolicyRuleException("가족돌봄휴가 정책에는 휴가 규칙(leaveRule)이 필수입니다.");
        }

        LeaveRuleDto leaveRule = details.getLeaveRule();

        // 2. 기본 부여 일수 필수 검사
        if (leaveRule.getDefaultDays() == null) {
            throw new InvalidPolicyRuleException("가족돌봄휴가 규칙에는 기본 부여 일수(defaultDays)가 필수입니다.");
        }

        // 3. 남녀고용평등법 제22조의2 - 가족돌봄휴가는 연간 최대 10일까지 가능
        if (leaveRule.getDefaultDays() > 10) {
            throw new InvalidPolicyRuleException(
                "법규 위반: 가족돌봄휴가는 연간 최대 10일까지 가능합니다. (남녀고용평등법 제22조의2)"
            );
        }

        // 4. 최소 기간 검증 (최소 1일 이상)
        if (leaveRule.getDefaultDays() <= 0) {
            throw new InvalidPolicyRuleException("가족돌봄휴가 기간은 최소 1일 이상이어야 합니다.");
        }

        // 5. 주기별 제한 검증 (연간 제한)
        if (leaveRule.getLimitPeriod() != null) {
            if (!leaveRule.getLimitPeriod().equals("YEARLY")) {
                throw new InvalidPolicyRuleException("가족돌봄휴가의 제한 주기(limitPeriod)는 'YEARLY'여야 합니다.");
            }
        }

        if (leaveRule.getMaxDaysPerPeriod() != null) {
            if (leaveRule.getMaxDaysPerPeriod() > 10) {
                throw new InvalidPolicyRuleException(
                    "법규 위반: 연간 최대 사용 일수(maxDaysPerPeriod)는 10일을 초과할 수 없습니다. (남녀고용평등법 제22조의2)"
                );
            }
            if (leaveRule.getMaxDaysPerPeriod() < 1) {
                throw new InvalidPolicyRuleException("연간 최대 사용 일수는 1일 이상이어야 합니다.");
            }
        }
    }
}
