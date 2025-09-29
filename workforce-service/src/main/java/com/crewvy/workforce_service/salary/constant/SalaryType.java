package com.crewvy.workforce_service.salary.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SalaryType {
    ALLOWANCE("ST001", "지급"),
    DEDUCTION("ST002", "공제");

    private final String codeValue;
    private final String codeName;

    public static SalaryType fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown SalaryType code: " + code));
    }

}
