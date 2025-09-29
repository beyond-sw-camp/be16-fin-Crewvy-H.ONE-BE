package com.crewvy.workforce_service.attendance.converter;

import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RequestStatusConverter implements AttributeConverter<RequestStatus, String> {
    @Override
    public String convertToDatabaseColumn(RequestStatus attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public RequestStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? RequestStatus.fromCode(dbData) : null;
    }
}
