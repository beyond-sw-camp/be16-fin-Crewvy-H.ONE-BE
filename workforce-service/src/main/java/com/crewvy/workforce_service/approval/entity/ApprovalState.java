package com.crewvy.workforce_service.approval.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ApprovalState {
    DRAFT("AS001", "임시저장"),
    PENDING("AS002", "진행중"),
    APPROVED("AS003", "승인"),
    REJECTED("AS004", "반려"),
    DISCARDED("AS005", "폐기");

    private final String codeValue;
    private final String codeName;

    public static ApprovalState fromCode(String codeValue) {
        return Arrays.stream(values())
                .filter(v -> v.getCodeValue().equals(codeValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ApprovalState code: " + codeValue));
    }
}