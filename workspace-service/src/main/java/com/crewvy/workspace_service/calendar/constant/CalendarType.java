package com.crewvy.workspace_service.calendar.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CalendarType {
    MEETING("CT001", "화상회의"),
    RESERVATION("CT002", "예약"),
    VACATION("CT003", "휴가"),
    BUSINESS_TRIP("CT004", "출장"),
    PERSONAL("CT005", "개인일정");

    private final String codeValue;
    private final String codeName;

    public static CalendarType fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown CalendarType code: " + code));
    }
}
