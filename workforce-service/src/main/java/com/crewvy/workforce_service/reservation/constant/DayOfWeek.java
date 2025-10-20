package com.crewvy.workforce_service.reservation.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DayOfWeek {

    SUNDAY(1, "일"),
    MONDAY(2, "월"),
    TUESDAY(4, "화"),
    WEDNESDAY(8, "수"),
    THURSDAY(16, "목"),
    FRIDAY(32, "금"),
    SATURDAY(64, "토");

    private final int codeValue;
    private final String codeName;

    public static DayOfWeek fromCode(int code) {
        return Arrays.stream(values())
                .filter(v -> v.getCodeValue() == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown DayOfWeek code: " + code));
    }
}
