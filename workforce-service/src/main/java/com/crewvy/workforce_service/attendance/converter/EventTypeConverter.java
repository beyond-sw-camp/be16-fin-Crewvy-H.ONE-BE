package com.crewvy.workforce_service.attendance.converter;

import com.crewvy.workforce_service.attendance.constant.EventType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EventTypeConverter implements AttributeConverter<EventType, String> {
    @Override
    public String convertToDatabaseColumn(EventType attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public EventType convertToEntityAttribute(String dbData) {
        return dbData != null ? EventType.fromCode(dbData) : null;
    }
}
