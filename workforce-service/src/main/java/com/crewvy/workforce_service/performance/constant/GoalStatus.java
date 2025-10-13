package com.crewvy.workforce_service.performance.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum GoalStatus {
    REQUESTED("GS001", "요청"),
    APPROVED("GS002", "승인"),
    REJECTED("GS003", "반려"),
    COMPLETED("GS004", "완료"),
    CANCELED("GS005", "취소");

    private final String codeValue;
    private final String codeName;

    public static GoalStatus fromCode(String codeValue) {
        return Arrays.stream(values())
                .filter(v -> v.getCodeValue().equals(codeValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown GoalStatus code: " + codeValue));
    }
}