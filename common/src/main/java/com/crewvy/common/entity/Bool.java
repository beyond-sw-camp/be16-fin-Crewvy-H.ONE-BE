package com.crewvy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum Bool {
    FALSE("N", "아니오"),
    TRUE("Y", "예");

    private final String codeValue;
    private final String codeName;

    public static Bool fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElse(null);
    }
}
