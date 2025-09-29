package com.crewvy.workforce_service.performance.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EvaluationTypeConverter implements AttributeConverter<EvaluationType, String> {

    @Override
    public String convertToDatabaseColumn(EvaluationType evaluationType) {
        if (evaluationType == null) {
            return null;
        }
        return evaluationType.getCodeValue(); // DB 저장 시 codeValue ("ET001") 저장
    }

    @Override
    public EvaluationType convertToEntityAttribute(String codeValue) {
        if (codeValue == null) {
            return null;
        }
        return EvaluationType.fromCode(codeValue); // DB 조회 시 codeValue로 Enum 생성
    }
}