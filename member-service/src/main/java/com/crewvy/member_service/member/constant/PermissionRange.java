package com.crewvy.member_service.member.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PermissionRange {
    INDIVIDUAL("PR001", "개인"),
    DEPARTMENT("PR002", "부서"),
    COMPANY("PR003", "전사"),
    SYSTEM("PR004", "시스템관리자");

    private final String codeValue;
    private final String codeName;

    public static PermissionRange fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PermissionRange code: " + code));
    }
}
