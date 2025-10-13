package com.crewvy.workforce_service.attendance.converter;

import com.crewvy.workforce_service.attendance.constant.DeviceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DeviceTypeConverter implements AttributeConverter<DeviceType, String> {
    @Override
    public String convertToDatabaseColumn(DeviceType attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public DeviceType convertToEntityAttribute(String dbData) {
        return dbData != null ? DeviceType.fromCode(dbData) : null;
    }
}
