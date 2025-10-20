package com.crewvy.workforce_service.reservation.converter;

import com.crewvy.workforce_service.reservation.constant.ReservationTypeStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReservationTypeStatusConverter implements AttributeConverter<ReservationTypeStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReservationTypeStatus attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public ReservationTypeStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? ReservationTypeStatus.fromCode(dbData) : null;
    }
}
