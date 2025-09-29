package com.crewvy.workforce_service.reservation.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ReservationStatus {
    REQUEST("RS001", "요청"),
    APPROVAL("RS002", "승인"),
    REJECT("RS003", "반려");

    private final String codeValue;
    private final String codeName;

    public static ReservationStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ReservationStatus code: " + code));
    }
}
