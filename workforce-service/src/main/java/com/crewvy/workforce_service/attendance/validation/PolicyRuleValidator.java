package com.crewvy.workforce_service.attendance.validation;

import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import com.crewvy.common.exception.InvalidPolicyRuleException;

/**
 * 정책 규칙의 유효성을 검증하는 Validator의 공통 인터페이스
 */
public interface PolicyRuleValidator {
    /**
     * 해당 정책 유형에 맞는 규칙들을 검증합니다.
     * @param details 검증할 규칙 상세 정보
     * @throws InvalidPolicyRuleException 유효성 검증 실패 시
     */
    void validate(PolicyRuleDetails details);
}
