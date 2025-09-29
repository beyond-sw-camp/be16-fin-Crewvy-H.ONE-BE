package com.crewvy.workforce_service.approval.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum LineStatus {
    WAITING("LS001", "대기"),
    PENDING("LS002", "진행중"),
    APPROVED("LS003", "승인"),
    REJECTED("LS004", "반려");

    private final String codeValue;
    private final String codeName;

    public static LineStatus fromCode(String codeValue) {
        return Arrays.stream(values())
                .filter(v -> v.getCodeValue().equals(codeValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown LineStatus code: " + codeValue));
    }
}