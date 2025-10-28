package com.crewvy.workforce_service.salary.converter;

import com.crewvy.workforce_service.salary.constant.PayType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PayTypeConverter implements AttributeConverter<PayType, String> {

    @Override
    public String convertToDatabaseColumn(PayType attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public PayType convertToEntityAttribute(String dbData) {
        return dbData != null ? PayType.fromCode(dbData) : null;
    }
}
