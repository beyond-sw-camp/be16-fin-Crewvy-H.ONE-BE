package com.crewvy.workforce_service.salary.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum HolidayRule {

    PREPAID("HR001", "전일 지급"),
    POSTPAID("HR002", "익일 지급");

    private final String codeValue;
    private final String codeName;

    public static HolidayRule fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown HolidayRule code: " + code));
    }
}
