package com.crewvy.workforce_service.salary.converter;

import com.crewvy.workforce_service.salary.constant.PeriodType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PeriodTypeConverter implements AttributeConverter<PeriodType, String> {

    @Override
    public String convertToDatabaseColumn(PeriodType attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public PeriodType convertToEntityAttribute(String dbData) {
        return dbData != null ? PeriodType.fromCode(dbData) : null;
    }
}
