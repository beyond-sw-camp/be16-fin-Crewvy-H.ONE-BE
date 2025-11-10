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
    CANCELED("GS004", "취소"),
    AWAITING_EVALUATION("GS005", "평가 대기"),
    SELF_EVAL_COMPLETED("GS006", "본인 평가 완료"),
    MANAGER_EVAL_COMPLETED("GS007", "최종 평가 완료");

    private final String codeValue;
    private final String codeName;

    public static GoalStatus fromCode(String codeValue) {
        return Arrays.stream(values())
                .filter(v -> v.getCodeValue().equals(codeValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown GoalStatus code: " + codeValue));
    }
}