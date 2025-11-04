package com.crewvy.workforce_service.attendance.validation;

import com.crewvy.common.exception.InvalidPolicyRuleException;
import com.crewvy.workforce_service.attendance.dto.rule.OvertimeRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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

        // 3. 휴게 규칙 타입별 필수 필드 검증
        var breakRule = details.getBreakRule();
        if (breakRule.getType() == null) {
            throw new InvalidPolicyRuleException("휴게 규칙의 타입(type)은 필수입니다.");
        }

        if ("FIXED".equals(breakRule.getType())) {
            // FIXED 타입: 고정 휴게 시작/종료 시각 필수
            if (breakRule.getFixedBreakStart() == null || breakRule.getFixedBreakEnd() == null) {
                throw new InvalidPolicyRuleException("FIXED 타입의 휴게 규칙에는 시작 및 종료 시각(fixedBreakStart, fixedBreakEnd)이 필수입니다.");
            }
        } else if ("AUTO".equals(breakRule.getType()) || "MANUAL".equals(breakRule.getType())) {
            // AUTO/MANUAL 타입: 기본 휴게 시간 필수
            if (breakRule.getDefaultBreakMinutesFor8Hours() == null) {
                throw new InvalidPolicyRuleException("AUTO/MANUAL 타입의 휴게 규칙에는 기본 휴게 시간(defaultBreakMinutesFor8Hours)이 필수입니다.");
            }
        }

        // 4. 법적 휴게시간 준수 여부 검사 (근로기준법 제54조)
        // FIXED 타입은 고정 휴게 시간으로 법정 최소 시간 자동 보장, AUTO/MANUAL만 검증
        if (!"FIXED".equals(breakRule.getType())) {
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

        // 4. 연장/야간/휴일 근무 규칙 검증 (OvertimeRuleDto)
        if (details.getOvertimeRule() != null) {
            validateOvertimeRule(details.getOvertimeRule());
        }
    }

    private void validateOvertimeRule(OvertimeRuleDto overtimeRule) {
        // 주 12시간 연장근무 한도 검증 (근로기준법 제53조)
        if (overtimeRule.getMaxWeeklyOvertimeMinutes() != null && overtimeRule.getMaxWeeklyOvertimeMinutes() > 720) {
            throw new InvalidPolicyRuleException("법규 위반: 주간 최대 연장근무 한도는 12시간(720분)을 초과할 수 없습니다.");
        }

        // 가산 수당률 법적 최소치 검증 (근로기준법 제56조)
        validateRate("연장근무 가산 수당률", overtimeRule.getOvertimeRate(), new BigDecimal("1.5"));
        validateRate("야간근무 가산 수당률", overtimeRule.getNightWorkRate(), new BigDecimal("1.5"));
        validateRate("휴일근무 가산 수당률", overtimeRule.getHolidayWorkRate(), new BigDecimal("1.5"));
        validateRate("휴일 연장근무 가산 수당률", overtimeRule.getHolidayOvertimeRate(), new BigDecimal("2.0"));
    }

    private void validateRate(String rateName, BigDecimal rate, BigDecimal minimumRate) {
        if (rate != null && rate.compareTo(minimumRate) < 0) {
            throw new InvalidPolicyRuleException(String.format("법규 위반: %s은(는) %s 이상이어야 합니다.", rateName, minimumRate));
        }
    }
}
