package com.crewvy.workforce_service.attendance.validation;

import com.crewvy.common.exception.InvalidPolicyRuleException;
import com.crewvy.workforce_service.attendance.dto.rule.LeaveRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import org.springframework.stereotype.Component;

/**
 * 연차유급휴가 정책 검증 로직.
 * 근로기준법 제60조에 따른 법적 요구사항을 검증합니다.
 */
@Component("annual_leaveValidator")
public class AnnualLeaveValidator implements PolicyRuleValidator {
    @Override
    public void validate(PolicyRuleDetails details) {
        if (details.getLeaveRule() == null) {
            throw new InvalidPolicyRuleException("연차 정책에는 휴가 규칙(leaveRule)이 필수입니다.");
        }

        LeaveRuleDto leaveRule = details.getLeaveRule();

        // 1. 기본 부여 일수 검증
        if (leaveRule.getDefaultDays() == null) {
            throw new InvalidPolicyRuleException("연차 규칙에는 기본 부여 일수(defaultDays)가 필수입니다.");
        }

        // 근로기준법 제60조 제1항 - 1년간 80% 이상 출근 시 15일
        if (leaveRule.getDefaultDays() < 15) {
            throw new InvalidPolicyRuleException("법규 위반: 연차 기본 부여 일수는 최소 15일 이상이어야 합니다. (근로기준법 제60조 제1항)");
        }

        // 2. 1년 미만 근로자 발생 일수 검증
        if (leaveRule.getFirstYearMaxAccrual() != null) {
            // 근로기준법 제60조 제2항 - 1개월 개근 시 1일 (최대 11일)
            if (leaveRule.getFirstYearMaxAccrual() > 11) {
                throw new InvalidPolicyRuleException("법규 위반: 1년 미만 근로자의 연차는 최대 11일입니다. (근로기준법 제60조 제2항)");
            }
            if (leaveRule.getFirstYearMaxAccrual() < 0) {
                throw new InvalidPolicyRuleException("1년 미만 근로자 최대 발생 일수는 0 이상이어야 합니다.");
            }
        }

        // 3. 발생 유형 검증
        if (leaveRule.getAccrualType() != null) {
            if (!leaveRule.getAccrualType().equals("ACCRUAL") && !leaveRule.getAccrualType().equals("MANUAL")) {
                throw new InvalidPolicyRuleException("발생 유형(accrualType)은 'ACCRUAL' 또는 'MANUAL'이어야 합니다.");
            }
        }

        // 4. 최소 신청 단위 검증
        if (leaveRule.getMinimumRequestUnit() != null) {
            if (!leaveRule.getMinimumRequestUnit().equals("DAY") &&
                !leaveRule.getMinimumRequestUnit().equals("HALF_DAY") &&
                !leaveRule.getMinimumRequestUnit().equals("HOUR")) {
                throw new InvalidPolicyRuleException("최소 신청 단위(minimumRequestUnit)는 'DAY', 'HALF_DAY', 'HOUR' 중 하나여야 합니다.");
            }
        }
    }
}
