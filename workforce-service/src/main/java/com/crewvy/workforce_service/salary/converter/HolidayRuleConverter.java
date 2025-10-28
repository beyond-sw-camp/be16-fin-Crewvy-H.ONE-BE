package com.crewvy.workforce_service.salary.converter;

import com.crewvy.workforce_service.salary.constant.HolidayRule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class HolidayRuleConverter implements AttributeConverter<HolidayRule, String> {

    @Override
    public String convertToDatabaseColumn(HolidayRule attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public HolidayRule convertToEntityAttribute(String dbData) {
        return dbData != null ? HolidayRule.fromCode(dbData) : null;
    }
}
