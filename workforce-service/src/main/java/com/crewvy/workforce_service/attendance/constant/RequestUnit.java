package com.crewvy.workforce_service.attendance.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RequestUnit {
    DAY("RU001", "일차"),
    HALF_DAY_AM("RU002", "오전 반차"),
    HALF_DAY_PM("RU003", "오후 반차"),
    TIME_OFF("RU004", "시차");

    private final String codeValue;
    private final String codeName;

    @JsonCreator
    public static RequestUnit fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code) || v.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown RequestUnit: " + code));
    }
}
