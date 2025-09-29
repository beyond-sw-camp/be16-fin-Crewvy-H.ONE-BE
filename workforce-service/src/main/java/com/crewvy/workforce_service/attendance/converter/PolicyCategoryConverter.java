package com.crewvy.workforce_service.attendance.converter;

import com.crewvy.workforce_service.attendance.constant.PolicyCategory;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PolicyCategoryConverter implements AttributeConverter<PolicyCategory, String> {
    @Override
    public String convertToDatabaseColumn(PolicyCategory attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public PolicyCategory convertToEntityAttribute(String dbData) {
        return dbData != null ? PolicyCategory.fromCode(dbData) : null;
    }
}
