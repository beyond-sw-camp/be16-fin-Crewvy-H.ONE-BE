package com.crewvy.workforce_service.salary.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PayType {

    ANNUAL("PT001" ,"연봉"),
    MONTHLY("PT002", "월급"),
    HOURLY("PT003", "시급");

    private final String codeValue;
    private final String codeName;

    public static PayType fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PayType code: " + code));
    }
}
