package com.crewvy.workspace_service.meeting.converter;

import com.crewvy.workspace_service.meeting.constant.MinuteStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MinuteStatusConverter implements AttributeConverter<MinuteStatus, String> {
    @Override
    public String convertToDatabaseColumn(MinuteStatus attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public MinuteStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? MinuteStatus.fromCode(dbData) : null;
    }
}
