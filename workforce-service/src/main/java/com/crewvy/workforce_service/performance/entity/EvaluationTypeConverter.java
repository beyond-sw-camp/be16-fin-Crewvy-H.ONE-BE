package com.crewvy.workforce_service.performance.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.stream.Stream;

@Converter
public class EvaluationTypeConverter implements AttributeConverter<EvaluationType, String> {

    @Override
    public String convertToDatabaseColumn(EvaluationType evaluationType) {
        if (evaluationType == null) {
            return null;
        }
        return evaluationType.getDescription();
    }

    @Override
    public EvaluationType convertToEntityAttribute(String description) {
        if (description == null) {
            return null;
        }
        return Stream.of(EvaluationType.values())
                .filter(c -> c.getDescription().equals(description))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}