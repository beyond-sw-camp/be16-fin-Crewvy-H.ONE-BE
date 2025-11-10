package com.crewvy.member_service.member.converter;

import com.crewvy.member_service.member.constant.AccountStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AccountStatusConverter implements AttributeConverter<AccountStatus, String> {
    @Override
    public String convertToDatabaseColumn(AccountStatus attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public AccountStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? AccountStatus.fromCode(dbData) : null;
    }
}
