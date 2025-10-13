package com.crewvy.workforce_service.attendance.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum EventType {
    CLOCK_IN("EVT001", "출근"),
    CLOCK_OUT("EVT002", "퇴근"),
    GO_OUT("EVT003", "외출"),
    COME_BACK("EVT004", "복귀"),
    BREAK_START("EVT005", "휴게 시작"),
    BREAK_END("EVT006", "휴게 종료");

    private final String codeValue;
    private final String codeName;

    @JsonCreator
    public static EventType fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(v -> v.getCodeValue().equals(code) || v.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown EventType: " + code));
    }
}
