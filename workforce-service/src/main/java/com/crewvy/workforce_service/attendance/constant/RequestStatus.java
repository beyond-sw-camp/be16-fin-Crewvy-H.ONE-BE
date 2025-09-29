package com.crewvy.workforce_service.attendance.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RequestStatus {
    PENDING("RS001", "대기"),
    APPROVED("RS002", "승인"),
    REJECTED("RS003", "반려"),
    CANCELED("RS004", "취소"),
    COMPLETED("RS005", "완료");

    private final String codeValue;
    private final String codeName;

    public static RequestStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown RequestStatus: " + code));
    }
}
