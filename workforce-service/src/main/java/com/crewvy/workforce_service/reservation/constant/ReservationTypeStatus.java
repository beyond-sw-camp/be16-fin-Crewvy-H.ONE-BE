package com.crewvy.workforce_service.reservation.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ReservationTypeStatus {
    AVAILABLE("RCS001", "사용가능"),
    RESERVED("RCS002", "사용중"),
    UNAVAILABLE("RCS003", "사용불가"),
    MAINTENANCE("RCS004", "점검중"),
    INACTIVE("RCS005", "비활성");


    private final String codeValue;
    private final String codeName;

    public static ReservationTypeStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ReservationTypeStatus code: " + code));
    }
}
