package com.crewvy.workforce_service.salary.converter;

import com.crewvy.workforce_service.salary.constant.SalaryStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SalaryStatusConverter implements AttributeConverter<SalaryStatus, String> {

    @Override
    public String convertToDatabaseColumn(SalaryStatus attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public SalaryStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? SalaryStatus.fromCode(dbData) : null;
    }
}
