package com.crewvy.workforce_service.attendance.converter;

import com.crewvy.common.exception.BusinessException;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class JsonToPolicyRuleDetailsConverter implements AttributeConverter<PolicyRuleDetails, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(PolicyRuleDetails attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new BusinessException("정책 규칙을 JSON으로 변환하는 데 실패했습니다.", e);
        }
    }

    @Override
    public PolicyRuleDetails convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, PolicyRuleDetails.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("JSON을 정책 규칙으로 변환하는 데 실패했습니다.", e);
        }
    }
}
