package com.crewvy.member_service.member.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum AccountStatus {
    ACTIVE("AS001", "정상"),
    INACTIVE("AS002", "비활성"),
    LOCK("AS003", "잠금");

    private final String codeValue;
    private final String codeName;

    public static AccountStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown AccountStatus code: " + code));
    }
}
