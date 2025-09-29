package com.crewvy.workforce_service.attendance.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BalanceTypeCodeConverter implements AttributeConverter<BalanceTypeCode, String> {
    @Override
    public String convertToDatabaseColumn(BalanceTypeCode attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public BalanceTypeCode convertToEntityAttribute(String dbData) {
        return dbData != null ? BalanceTypeCode.fromCode(dbData) : null;
    }
}
