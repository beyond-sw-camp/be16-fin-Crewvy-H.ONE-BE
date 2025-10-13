package com.crewvy.member_service.member.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum Action {
    CREATE("AC001", "생성"),
    READ("AC002", "조회"),
    UPDATE("AC003", "수정"),
    DELETE("AC004", "삭제");

    private final String codeValue;
    private final String codeName;

    public static Action fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Action code: " + code));
    }
}
