package com.crewvy.workforce_service.performance.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.stream.Stream;

@Converter
public class GoalStatusConverter implements AttributeConverter<GoalStatus, String> {

    @Override
    public String convertToDatabaseColumn(GoalStatus goalStatus) {
        if (goalStatus == null) {
            return null;
        }
        return goalStatus.getDescription();
    }

    @Override
    public GoalStatus convertToEntityAttribute(String description) {
        if (description == null) {
            return null;
        }
        return Stream.of(GoalStatus.values())
                .filter(c -> c.getDescription().equals(description))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}