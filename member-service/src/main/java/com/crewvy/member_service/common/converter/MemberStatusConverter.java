package com.crewvy.member_service.common.converter;

import com.crewvy.member_service.common.constant.MemberStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MemberStatusConverter implements AttributeConverter<MemberStatus, String> {
    @Override
    public String convertToDatabaseColumn(MemberStatus attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public MemberStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? MemberStatus.fromCode(dbData) : null;
    }
}
