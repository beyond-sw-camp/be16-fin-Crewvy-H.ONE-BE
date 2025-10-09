package com.crewvy.workforce_service.attendance.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DeviceType {
    LAPTOP("DT001", "노트북"),
    MOBILE("DT002", "모바일 기기");

    private final String codeValue;
    private final String codeName;

    @JsonCreator
    public static DeviceType fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(v -> v.getCodeValue().equals(code) || v.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown DeviceType: " + code));
    }
}
