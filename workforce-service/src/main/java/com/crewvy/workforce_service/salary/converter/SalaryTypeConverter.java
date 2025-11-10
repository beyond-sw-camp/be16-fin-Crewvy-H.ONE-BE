package com.crewvy.workforce_service.salary.converter;

import com.crewvy.workforce_service.salary.constant.SalaryType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SalaryTypeConverter implements AttributeConverter<SalaryType, String> {

    @Override
    public String convertToDatabaseColumn(SalaryType attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public SalaryType convertToEntityAttribute(String dbData) {
        return dbData != null ? SalaryType.fromCode(dbData) : null;
    }

}
