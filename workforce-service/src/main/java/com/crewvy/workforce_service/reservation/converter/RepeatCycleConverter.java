package com.crewvy.workforce_service.reservation.converter;

import com.crewvy.workforce_service.reservation.constant.RepeatCycle;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RepeatCycleConverter implements AttributeConverter<RepeatCycle, String> {

    @Override
    public String convertToDatabaseColumn(RepeatCycle attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public RepeatCycle convertToEntityAttribute(String dbData) {
        return dbData != null ? RepeatCycle.fromCode(dbData) : null;
    }
}
