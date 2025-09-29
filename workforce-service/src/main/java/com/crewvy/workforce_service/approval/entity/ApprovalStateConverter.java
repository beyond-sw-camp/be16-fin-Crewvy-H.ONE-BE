package com.crewvy.workforce_service.approval.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ApprovalStateConverter implements AttributeConverter<ApprovalState, String> {

    @Override
    public String convertToDatabaseColumn(ApprovalState approvalState) {
        if (approvalState == null) return null;
        return approvalState.getCodeValue(); // DB 저장 시: codeValue ("AS001") 저장
    }

    @Override
    public ApprovalState convertToEntityAttribute(String codeValue) {
        if (codeValue == null) return null;
        return ApprovalState.fromCode(codeValue); // DB 조회 시: codeValue로 Enum 생성
    }
}