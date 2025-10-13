package com.crewvy.member_service.member.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum EmploymentType {
    FULL("ET001", "정규직"),
    CONTRACT("ET002", "계약직"),
    INTERN("ET003", "인턴"),
    ETC("ET004", "기타");

    private final String codeValue;
    private final String codeName;

    public static EmploymentType fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown EmploymentType code: " + code));
    }
}
