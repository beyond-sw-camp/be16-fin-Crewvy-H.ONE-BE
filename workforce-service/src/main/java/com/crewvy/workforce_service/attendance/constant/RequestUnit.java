package com.crewvy.workforce_service.attendance.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RequestUnit {
    DAY("RU001", "일차"),
    HALF_DAY_AM("RU002", "오전 반차"),
    HALF_DAY_PM("RU003", "오후 반차");

    private final String codeValue;
    private final String codeName;

    public static RequestUnit fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown RequestUnit: " + code));
    }
}
