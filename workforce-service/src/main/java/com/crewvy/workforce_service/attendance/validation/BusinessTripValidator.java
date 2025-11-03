package com.crewvy.workforce_service.attendance.validation;

import com.crewvy.common.exception.InvalidPolicyRuleException;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import com.crewvy.workforce_service.attendance.dto.rule.TripRuleDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 출장 정책 검증 로직.
 * 출장 관련 비즈니스 규칙을 검증합니다.
 */
@Component("business_tripValidator")
public class BusinessTripValidator implements PolicyRuleValidator {

    @Override
    public void validate(PolicyRuleDetails details) {
        // 1. 필수 규칙 블록 존재 여부 검사
        if (details.getTripRule() == null) {
            throw new InvalidPolicyRuleException("출장 정책에는 출장 규칙(tripRule)이 필수입니다.");
        }

        TripRuleDto tripRule = details.getTripRule();

        // 2. 출장 유형 필수 검사
        if (tripRule.getType() == null || tripRule.getType().trim().isEmpty()) {
            throw new InvalidPolicyRuleException("출장 규칙에는 출장 유형(type)이 필수입니다.");
        }

        // 3. 금액 필드 음수 검증
        if (tripRule.getPerDiemAmount() != null && tripRule.getPerDiemAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPolicyRuleException("일비(perDiemAmount)는 음수일 수 없습니다.");
        }

        if (tripRule.getAccommodationLimit() != null && tripRule.getAccommodationLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPolicyRuleException("숙박비 한도(accommodationLimit)는 음수일 수 없습니다.");
        }

        if (tripRule.getTransportationLimit() != null && tripRule.getTransportationLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPolicyRuleException("교통비 한도(transportationLimit)는 음수일 수 없습니다.");
        }
    }
}
