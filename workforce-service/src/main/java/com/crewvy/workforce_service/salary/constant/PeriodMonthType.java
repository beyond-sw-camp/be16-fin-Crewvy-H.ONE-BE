package com.crewvy.workforce_service.salary.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PeriodMonthType {

    PREVIOUS_MONTH("PMT001", "전월"),
    CURRENT_MONTH("PMT001", "당월"),
    END_OF_MONTH("PMT001", "말일");

    private final String codeValue;
    private final String codeName;

    public static PeriodMonthType fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PeriodMonthType code: " + code));
    }
}
