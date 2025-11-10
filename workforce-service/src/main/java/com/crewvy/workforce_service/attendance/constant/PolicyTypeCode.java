package com.crewvy.workforce_service.attendance.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PolicyTypeCode {
    // 법적으로 필수 휴가 (모두 부여 일수 차감 필요)
    ANNUAL_LEAVE("PTC001", "연차유급휴가", true, null), // 근속 기간 기반이라 최소값 없음
    MATERNITY_LEAVE("PTC002", "출산전후휴가", true, 90.0), // 근로기준법 제74조 (90일)
    PATERNITY_LEAVE("PTC003", "배우자 출산휴가", true, 10.0), // 남녀고용평등법 제18조의2 (10일)
    CHILDCARE_LEAVE("PTC004", "육아휴직", true, null), // 개별 승인, 최소값 없음
    FAMILY_CARE_LEAVE("PTC005", "가족돌봄휴가", true, null), // 개별 승인, 최소값 없음
    MENSTRUAL_LEAVE("PTC006", "생리휴가", true, 12.0), // 근로기준법 제73조 (월 1일 * 12개월)

    // 근로시간 관련 (부여 일수 개념 없음, 승인만 필요)
    STANDARD_WORK("PTC101", "기본근무", false, null),
    BUSINESS_TRIP("PTC102", "출장", false, null),
    OVERTIME("PTC103", "연장근무", false, null),
    NIGHT_WORK("PTC104", "야간근무", false, null),
    HOLIDAY_WORK("PTC105", "휴일근무", false, null);

    private final String codeValue;
    private final String codeName;
    private final boolean isBalanceDeductible;
    private final Double legalMinimumDays; // 법정 최소 일수 (null이면 최소값 없음)

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
