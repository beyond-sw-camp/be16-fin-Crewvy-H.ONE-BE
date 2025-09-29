package com.crewvy.workforce_service.attendance.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PolicyTypeCode {
    // 법적으로 필수 휴가
    ANNUAL_LEAVE("PTC001", "연차유급휴가"),
    MATERNITY_LEAVE("PTC002", "출산전후휴가"),
    PATERNITY_LEAVE("PTC003", "배우자 출산휴가"),
    CHILDCARE_LEAVE("PTC004", "육아휴직"),
    FAMILY_CARE_LEAVE("PTC005", "가족돌봄휴가"),
    MENSTRUAL_LEAVE("PTC006", "생리휴가"),

    // 근로시간 관련
    BUSINESS_TRIP("PTC101", "출장"),
    OVERTIME("PTC102", "연장근무"),
    NIGHT_WORK("PTC103", "야간근무"),
    HOLIDAY_WORK("PTC104", "휴일근무");

    private final String codeValue;
    private final String codeName;

    public static PolicyTypeCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PolicyTypeCode: " + code));
    }
}
