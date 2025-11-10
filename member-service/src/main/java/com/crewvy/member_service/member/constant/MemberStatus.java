package com.crewvy.member_service.member.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum MemberStatus {
    WORKING("MS001", "재직"),
    LEAVE("MS002", "휴직"),
    DETACHMENT("MS003", "파견"),
    DELETED("MS004", "삭제");

    private final String codeValue;
    private final String codeName;

    public static MemberStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown MemberStatus code: " + code));
    }
}
