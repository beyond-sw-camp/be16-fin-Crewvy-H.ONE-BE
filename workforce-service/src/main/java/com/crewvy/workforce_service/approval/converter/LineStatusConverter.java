package com.crewvy.workforce_service.approval.converter;

import com.crewvy.workforce_service.approval.constant.LineStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class LineStatusConverter implements AttributeConverter<LineStatus, String> {

    @Override
    public String convertToDatabaseColumn(LineStatus lineStatus) {
        if (lineStatus == null) return null;
        return lineStatus.getCodeValue();
    }

    @Override
    public LineStatus convertToEntityAttribute(String codeValue) {
        if (codeValue == null) return null;
        return LineStatus.fromCode(codeValue);
    }
}