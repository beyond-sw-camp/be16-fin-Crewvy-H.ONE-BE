package com.crewvy.workforce_service.attendance.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PolicyTypeCode {
    // 법적으로 필수 휴가
    ANNUAL_LEAVE("PTC001", "연차유급휴가", true),
    MATERNITY_LEAVE("PTC002", "출산전후휴가", false),
    PATERNITY_LEAVE("PTC003", "배우자 출산휴가", false),
    CHILDCARE_LEAVE("PTC004", "육아휴직", false),
    FAMILY_CARE_LEAVE("PTC005", "가족돌봄휴가", false),
    MENSTRUAL_LEAVE("PTC006", "생리휴가", false),

    // 근로시간 관련
    STANDARD_WORK("PTC101", "기본근무", false),
    BUSINESS_TRIP("PTC102", "출장", false),
    OVERTIME("PTC103", "연장근무", false),
    NIGHT_WORK("PTC104", "야간근무", false),
    HOLIDAY_WORK("PTC105", "휴일근무", false);

    private final String codeValue;
    private final String codeName;
    private final boolean isBalanceDeductible;

    @JsonValue
    public String getCodeValue() {
        return codeValue;
    }

    @JsonCreator
    public static PolicyTypeCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PolicyTypeCode: " + code));
    }
}
