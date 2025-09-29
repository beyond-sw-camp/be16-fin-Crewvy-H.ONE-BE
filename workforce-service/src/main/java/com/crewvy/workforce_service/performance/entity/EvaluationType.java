package com.crewvy.workforce_service.performance.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum EvaluationType {
    SELF("ET001", "본인 평가"),
    SUPERVISOR("ET002", "상급자 평가");

    private final String codeValue;
    private final String codeName;

    public static EvaluationType fromCode(String codeValue) {
        return Arrays.stream(values())
                .filter(v -> v.getCodeValue().equals(codeValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown EvaluationType code: " + codeValue));
    }
}