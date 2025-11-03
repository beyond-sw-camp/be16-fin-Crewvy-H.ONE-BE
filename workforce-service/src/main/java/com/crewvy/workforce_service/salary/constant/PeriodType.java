package com.crewvy.workforce_service.salary.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PeriodType {

    LAST_MONTH_FULL("PT001", "전월"),
    THIS_MONTH_FULL("PT002", "당월"),
    SPECIFIC("PT003", "사용자지정");

    private final String codeValue;
    private final String codeName;

    public static PeriodType fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PeriodType code: " + code));
    }
}
