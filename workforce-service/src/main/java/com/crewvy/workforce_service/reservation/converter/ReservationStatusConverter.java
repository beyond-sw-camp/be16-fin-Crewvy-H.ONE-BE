package com.crewvy.workforce_service.reservation.converter;

import com.crewvy.workforce_service.reservation.constant.ReservationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReservationStatusConverter implements AttributeConverter<ReservationStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReservationStatus attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public ReservationStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? ReservationStatus.fromCode(dbData) : null;
    }
}
