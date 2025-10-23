package com.crewvy.workforce_service.performance.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum TeamGoalStatus {
    PROCESSING("TS001", "진행중"),
    CANCELED("TS002", "삭제"),
    AWAITING_EVALUATION("TS003", "평가 대기"),
    EVALUATION_COMPLETED("TS004", "평가 완료");

    private final String codeValue;
    private final String codeName;

    public static TeamGoalStatus fromCode(String codeValue) {
        return Arrays.stream(values())
                .filter(v -> v.getCodeValue().equals(codeValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown TeamGoalStatus code: " + codeValue));
    }
}
