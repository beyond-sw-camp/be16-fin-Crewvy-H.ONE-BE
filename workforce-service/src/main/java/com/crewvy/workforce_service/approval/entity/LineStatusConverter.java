package com.crewvy.workforce_service.approval.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class LineStatusConverter implements AttributeConverter<LineStatus, String> {

    @Override
    public String convertToDatabaseColumn(LineStatus lineStatus) {
        if (lineStatus == null) {
            return null;
        }
        return lineStatus.getDescription();
    }

    @Override
    public LineStatus convertToEntityAttribute(String description) {
        if (description == null) {
            return null;
        }
        return java.util.stream.Stream.of(LineStatus.values())
                .filter(c -> c.getDescription().equals(description))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}