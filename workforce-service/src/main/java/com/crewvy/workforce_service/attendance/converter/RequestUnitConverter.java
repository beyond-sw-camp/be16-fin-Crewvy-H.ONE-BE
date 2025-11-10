package com.crewvy.workforce_service.attendance.converter;

import com.crewvy.workforce_service.attendance.constant.RequestUnit;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RequestUnitConverter implements AttributeConverter<RequestUnit, String> {
    @Override
    public String convertToDatabaseColumn(RequestUnit attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public RequestUnit convertToEntityAttribute(String dbData) {
        return dbData != null ? RequestUnit.fromCode(dbData) : null;
    }
}
