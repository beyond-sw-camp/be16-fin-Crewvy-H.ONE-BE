package com.crewvy.workforce_service.reservation.converter;

import com.crewvy.workforce_service.reservation.constant.DayOfWeek;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DayOfWeekConverter implements AttributeConverter<DayOfWeek, Integer> {

    @Override
    public Integer convertToDatabaseColumn(DayOfWeek attribute) {
        return attribute != null ? attribute.getCodeValue() : 0;
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Integer dbData) {
        return dbData != null ? DayOfWeek.fromCode(dbData) : null;
    }
}
