package com.crewvy.workforce_service.approval.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RequirementType {
    TITLE("RT001", "직책"),
    MEMBER_POSITION("RT002", "직원");

    private final String codeValue;
    private final String codeName;

    public static RequirementType fromCode(String codeValue) {
        return Arrays.stream(values())
                .filter(v -> v.getCodeValue().equals(codeValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown RequirementType code: " + codeValue));
    }
}