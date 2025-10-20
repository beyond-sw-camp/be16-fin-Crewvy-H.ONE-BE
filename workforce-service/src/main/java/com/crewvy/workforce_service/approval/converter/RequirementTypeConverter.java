package com.crewvy.workforce_service.approval.converter;

import com.crewvy.workforce_service.approval.constant.RequirementType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RequirementTypeConverter implements AttributeConverter<RequirementType, String> {
    @Override
    public String convertToDatabaseColumn(RequirementType requirementType ) {
        if (requirementType == null) return null;
        return requirementType.getCodeValue();
    }

    @Override
    public RequirementType convertToEntityAttribute(String codeValue) {
        if (codeValue == null) return null;
        return RequirementType.fromCode(codeValue);
    }
}
