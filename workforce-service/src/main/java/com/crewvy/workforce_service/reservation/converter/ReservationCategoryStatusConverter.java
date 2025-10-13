package com.crewvy.workforce_service.reservation.converter;

import com.crewvy.workforce_service.reservation.constant.ReservationCategoryStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReservationCategoryStatusConverter implements AttributeConverter<ReservationCategoryStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReservationCategoryStatus attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public ReservationCategoryStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? ReservationCategoryStatus.fromCode(dbData) : null;
    }
}
