package com.crewvy.workforce_service.attendance.converter;

import com.crewvy.workforce_service.attendance.constant.PolicyScopeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PolicyScopeTypeConverter implements AttributeConverter<PolicyScopeType, String> {
    @Override
    public String convertToDatabaseColumn(PolicyScopeType attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public PolicyScopeType convertToEntityAttribute(String dbData) {
        return dbData != null ? PolicyScopeType.fromCode(dbData) : null;
    }
}
