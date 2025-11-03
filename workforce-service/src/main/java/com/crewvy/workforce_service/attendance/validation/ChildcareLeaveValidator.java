package com.crewvy.workforce_service.attendance.validation;

import com.crewvy.common.exception.InvalidPolicyRuleException;
import com.crewvy.workforce_service.attendance.dto.rule.LeaveRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import org.springframework.stereotype.Component;

/**
 * 육아휴직 정책 검증 로직.
 * 남녀고용평등법 제19조에 따른 법적 요구사항을 검증합니다.
 */
@Component("childcare_leaveValidator")
public class ChildcareLeaveValidator implements PolicyRuleValidator {

    @Override
    public void validate(PolicyRuleDetails details) {
        // 1. 필수 규칙 블록 존재 여부 검사
        if (details.getLeaveRule() == null) {
            throw new InvalidPolicyRuleException("육아휴직 정책에는 휴가 규칙(leaveRule)이 필수입니다.");
        }

        LeaveRuleDto leaveRule = details.getLeaveRule();

        // 2. 기본 부여 일수 필수 검사
        if (leaveRule.getDefaultDays() == null) {
            throw new InvalidPolicyRuleException("육아휴직 규칙에는 기본 부여 일수(defaultDays)가 필수입니다.");
        }

        // 3. 남녀고용평등법 제19조 - 육아휴직은 최대 1년(365일)까지 가능
        if (leaveRule.getDefaultDays() > 365) {
            throw new InvalidPolicyRuleException(
                "법규 위반: 육아휴직은 최대 1년(365일)까지 가능합니다. (남녀고용평등법 제19조)"
            );
        }

        // 4. 최소 기간 검증 (최소 1일 이상)
        if (leaveRule.getDefaultDays() <= 0) {
            throw new InvalidPolicyRuleException("육아휴직 기간은 최소 1일 이상이어야 합니다.");
        }

        // 5. 분할 사용 규칙 검증
        if (leaveRule.getMaxSplitCount() != null) {
            if (leaveRule.getMaxSplitCount() < 1) {
                throw new InvalidPolicyRuleException("최대 분할 횟수(maxSplitCount)는 1 이상이어야 합니다.");
            }
            // 실무적으로 분할은 최대 3회 정도가 적절
            if (leaveRule.getMaxSplitCount() > 10) {
                throw new InvalidPolicyRuleException("최대 분할 횟수(maxSplitCount)는 10 이하로 설정하는 것을 권장합니다.");
            }
        }

        // 6. 최소 연속 사용 일수 검증
        if (leaveRule.getMinConsecutiveDays() != null) {
            if (leaveRule.getMinConsecutiveDays() < 1) {
                throw new InvalidPolicyRuleException("최소 연속 사용 일수(minConsecutiveDays)는 1 이상이어야 합니다.");
            }
            // 최소 연속 일수가 전체 기간을 초과할 수 없음
            if (leaveRule.getDefaultDays() != null && leaveRule.getMinConsecutiveDays() > leaveRule.getDefaultDays()) {
                throw new InvalidPolicyRuleException("최소 연속 사용 일수는 전체 육아휴직 기간을 초과할 수 없습니다.");
            }
        }
    }
}
