package com.crewvy.workforce_service.salary.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PayDayType {

    SPECIFIC_DAY("PDT001", "특정일"),
    END_OF_MONTH("PDT002", "말일");

    private final String codeValue;
    private final String codeName;

    public static PayDayType fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PayDayType code: " + code));
    }
}
