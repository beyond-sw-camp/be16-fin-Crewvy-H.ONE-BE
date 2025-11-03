package com.crewvy.workforce_service.salary.converter;

import com.crewvy.workforce_service.salary.constant.PeriodMonthType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PeriodMonthTypeConverter implements AttributeConverter<PeriodMonthType, String> {

    @Override
    public String convertToDatabaseColumn(PeriodMonthType attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public PeriodMonthType convertToEntityAttribute(String dbData) {
        return dbData != null ? PeriodMonthType.fromCode(dbData) : null;
    }
}
