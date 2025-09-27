package com.crewvy.workforce_service.performance.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GradeConverter implements AttributeConverter<Grade, Integer> { // Double -> Integer

    @Override
    public Integer convertToDatabaseColumn(Grade grade) {
        if (grade == null) {
            return null;
        }
        return grade.getScore(); // DB에 저장 시: Enum에서 정수 점수를 추출
    }

    @Override
    public Grade convertToEntityAttribute(Integer score) {
        if (score == null) {
            return null;
        }
        return Grade.fromScore(score); // DB에서 조회 시: 정수 점수로 Enum을 생성
    }
}