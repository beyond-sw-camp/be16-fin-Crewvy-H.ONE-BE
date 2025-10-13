package com.crewvy.workforce_service.attendance.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PolicyCategory {
    ABSENCE("PC001", "부재"),
    WORK_SCHEDULE("PC002", "근무일정");

    private final String codeValue;
    private final String codeName;

    @JsonCreator
    public static PolicyCategory fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PolicyCategory: " + code));
    }
}
