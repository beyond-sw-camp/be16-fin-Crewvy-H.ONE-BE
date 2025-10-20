package com.crewvy.workforce_service.attendance.converter;

import com.crewvy.workforce_service.attendance.constant.AttendanceStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AttendanceStatusConverter implements AttributeConverter<AttendanceStatus, String> {
    @Override
    public String convertToDatabaseColumn(AttendanceStatus attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public AttendanceStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? AttendanceStatus.fromCode(dbData) : null;
    }
}
