package com.crewvy.workforce_service.salary.converter;

import com.crewvy.workforce_service.salary.constant.PayDayType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PayDayTypeConverter implements AttributeConverter<PayDayType, String> {

    @Override
    public String convertToDatabaseColumn(PayDayType attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public PayDayType convertToEntityAttribute(String dbData) {
        return dbData != null ? PayDayType.fromCode(dbData) : null;
    }
}
