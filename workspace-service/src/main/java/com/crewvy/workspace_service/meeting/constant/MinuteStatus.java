package com.crewvy.workspace_service.meeting.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum MinuteStatus {
    PENDING("MN001", "대기"),
    PROCESSING("MN002", "처리중"),
    COMPLETED("MN003", "완료"),
    FAILED("MN999", "실패");

    private final String codeValue;
    private final String codeName;

    public static MinuteStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown MemberStatus code: " + code));
    }
}
