package com.crewvy.workforce_service.attendance.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PolicyScopeType {
    COMPANY("PST001", "회사"),
    ORGANIZATION("PST002", "조직"),
    MEMBER("PST003", "개인"),
    MEMBER_POSITION("PST004", "직책");

    private final String codeValue;
    private final String codeName;

    @JsonCreator
    public static PolicyScopeType fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(v -> v.getCodeValue().equals(code) || v.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PolicyScopeType: " + code));
    }
}