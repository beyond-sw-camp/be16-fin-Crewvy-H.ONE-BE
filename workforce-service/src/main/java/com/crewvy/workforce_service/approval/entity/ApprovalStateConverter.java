package com.crewvy.workforce_service.approval.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ApprovalStateConverter implements AttributeConverter<ApprovalState, String> {

    @Override
    public String convertToDatabaseColumn(ApprovalState approvalState) {
        if (approvalState == null) {
            return null;
        }
        return approvalState.getDescription();
    }

    @Override
    public ApprovalState convertToEntityAttribute(String description) {
        if (description == null) {
            return null;
        }
        // 이 부분은 Enum에 fromDescription 메서드를 만들어 처리하거나,
        // 아래처럼 직접 스트림을 사용해 처리할 수 있습니다.
        return java.util.stream.Stream.of(ApprovalState.values())
                .filter(c -> c.getDescription().equals(description))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}