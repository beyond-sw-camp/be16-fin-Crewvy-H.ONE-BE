package com.crewvy.workforce_service.reservation.converter;

import com.crewvy.workforce_service.reservation.constant.ReservationRequestStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReservationRequestStatusConverter implements AttributeConverter<ReservationRequestStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReservationRequestStatus attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public ReservationRequestStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? ReservationRequestStatus.fromCode(dbData) : null;
    }
}
