package com.crewvy.workforce_service.reservation.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ReservationRequestStatus {
    REQUEST("RRS001", "요청"),
    APPROVAL("RRS002", "승인"),
    REJECT("RRS003", "반려");

    private final String codeValue;
    private final String codeName;

    public static ReservationRequestStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ReservationRequestStatus code: " + code));
    }
}
