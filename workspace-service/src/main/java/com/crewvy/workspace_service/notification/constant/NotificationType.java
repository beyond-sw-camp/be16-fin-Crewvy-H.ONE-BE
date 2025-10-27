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
    APPROVAL("NT004", "전자결재"),
    TEAM_GOAL("NT005", "팀 목표"),
    GOAL("NT006", "개인 목표"),
    GOAL_REQUEST("NT007", "목표 요청"),
    GOAL_EVALUATION("NT008", "개인 평가 상세"),
    TEAM_EVALUATION("NT009", "팀 평가 상세");

    private final String codeValue;
    private final String codeName;

    public static NotificationType fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown NotificationType code: " + code));
    }
}
