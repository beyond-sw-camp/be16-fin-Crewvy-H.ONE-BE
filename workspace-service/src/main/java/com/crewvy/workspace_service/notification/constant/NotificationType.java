package com.crewvy.workspace_service.notification.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum NotificationType {
    VIDEO_CONFERENCE_INVITE("NT001", "화상회의초대"),
    VIDEO_CONFERENCE_REMIND("NT002", "회의시작 10분전"),
    POST_NEW_COMMENT("NT003", "새로운 댓글"),
    APPROVAL("NT004", "전자결재");

    private final String codeValue;
    private final String codeName;

    public static NotificationType fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown MemberStatus code: " + code));
    }
}
