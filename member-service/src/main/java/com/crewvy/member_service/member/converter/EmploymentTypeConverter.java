package com.crewvy.member_service.member.converter;

import com.crewvy.member_service.member.constant.EmploymentType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EmploymentTypeConverter implements AttributeConverter<EmploymentType, String> {
    @Override
    public String convertToDatabaseColumn(EmploymentType attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public EmploymentType convertToEntityAttribute(String dbData) {
        return dbData != null ? EmploymentType.fromCode(dbData) : null;
    }
}
