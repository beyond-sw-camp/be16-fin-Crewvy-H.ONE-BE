package com.crewvy.workforce_service.attendance.converter;

import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PolicyTypeCodeConverter implements AttributeConverter<PolicyTypeCode, String> {
    @Override
    public String convertToDatabaseColumn(PolicyTypeCode attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public PolicyTypeCode convertToEntityAttribute(String dbData) {
        return dbData != null ? PolicyTypeCode.fromCode(dbData) : null;
    }
}
