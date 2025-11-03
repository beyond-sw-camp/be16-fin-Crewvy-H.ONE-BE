package com.crewvy.workspace_service.calendar.converter;

import com.crewvy.workspace_service.calendar.constant.CalendarType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CalendarTypeConverter implements AttributeConverter<CalendarType, String> {
    @Override
    public String convertToDatabaseColumn(CalendarType attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public CalendarType convertToEntityAttribute(String dbData) {
        return dbData != null ? CalendarType.fromCode(dbData) : null;
    }
}
