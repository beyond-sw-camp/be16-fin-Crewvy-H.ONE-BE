package com.crewvy.member_service.member.converter;

import com.crewvy.member_service.member.constant.Action;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ActionConverter implements AttributeConverter<Action, String> {
    @Override
    public String convertToDatabaseColumn(Action attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public Action convertToEntityAttribute(String dbData) {
        return dbData != null ? Action.fromCode(dbData) : null;
    }
}
