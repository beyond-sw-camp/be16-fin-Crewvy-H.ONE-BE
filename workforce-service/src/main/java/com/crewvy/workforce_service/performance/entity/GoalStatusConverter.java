package com.crewvy.workforce_service.performance.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GoalStatusConverter implements AttributeConverter<GoalStatus, String> {

    @Override
    public String convertToDatabaseColumn(GoalStatus goalStatus) {
        if (goalStatus == null) {
            return null;
        }
        return goalStatus.getCodeValue(); // DB 저장 시 codeValue ("GS001") 저장
    }

    @Override
    public GoalStatus convertToEntityAttribute(String codeValue) {
        if (codeValue == null) {
            return null;
        }
        return GoalStatus.fromCode(codeValue); // DB 조회 시 codeValue로 Enum 생성
    }
}