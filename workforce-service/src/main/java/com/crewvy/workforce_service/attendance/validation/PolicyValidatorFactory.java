package com.crewvy.workforce_service.attendance.validation;

import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PolicyValidatorFactory {
    // Spring이 PolicyRuleValidator를 구현한 모든 Bean을 자동으로 주입해줍니다.
    private final Map<String, PolicyRuleValidator> validators;

    public PolicyRuleValidator getValidator(PolicyTypeCode typeCode) {
        // Bean 이름 규칙을 "타입코드(소문자) + Validator"로 정합니다.
        // 예: STANDARD_WORK -> standard_workValidator
        String beanName = typeCode.name().toLowerCase() + "Validator";
        PolicyRuleValidator validator = validators.get(beanName);

        if (validator == null) {
            // 특정 검증기가 없는 경우, 아무것도 하지 않는 기본 검증기를 반환
            return details -> {}; // No-op validator
        }
        return validator;
    }
}
