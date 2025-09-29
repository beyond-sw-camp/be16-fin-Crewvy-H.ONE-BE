package com.crewvy.workforce_service.attendance.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum BalanceTypeCode {
    ANNUAL_LEAVE("BT001", "연차유급휴가"),
    MATERNITY_LEAVE("BT002", "출산전후휴가"),
    PATERNITY_LEAVE("BT003", "배우자 출산휴가"),
    CHILDCARE_LEAVE("BT004", "육아휴직"),
    FAMILY_CARE_LEAVE("BT005", "가족돌봄휴가"),
    MENSTRUAL_LEAVE("BT006", "생리휴가");

    private final String codeValue;
    private final String codeName;

    public static BalanceTypeCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown BalanceTypeCode: " + code));
    }
}
