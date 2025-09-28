package com.crewvy.workspace_service.notification.converter;

import com.crewvy.workspace_service.notification.constant.NotificationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class NotificationTypeConverter implements AttributeConverter<NotificationType, String> {
    @Override
    public String convertToDatabaseColumn(NotificationType attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public NotificationType convertToEntityAttribute(String dbData) {
        return dbData != null ? NotificationType.fromCode(dbData) : null;
    }
}
