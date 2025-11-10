package com.crewvy.workforce_service.reservation.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ReservationStatus {
    BEFORE("RS001", "이용 전"),
    USED("RS002", "이용 완료");

    private final String codeValue;
    private final String codeName;

    public static ReservationStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ReservationStatus code: " + code));
    }
}
