package com.crewvy.workspace_service.meeting.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum VideoConferenceStatus {
    WAITING("VC001", "대기"),
    IN_PROGRESS("VC002", "회의중"),
    ENDED("VC003", "종료");

    private final String codeValue;
    private final String codeName;

    public static VideoConferenceStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown MemberStatus code: " + code));
    }
}
