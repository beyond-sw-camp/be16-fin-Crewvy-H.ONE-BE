package com.crewvy.workforce_service.attendance.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 일일 근태 상태
 * 각 날짜별로 직원이 실제로 무엇을 했는지 표현
 */
@Getter
@AllArgsConstructor
public enum AttendanceStatus {
    // 출근 관련
    NORMAL_WORK("AS001", "정상 출근", true),
    BUSINESS_TRIP("AS002", "출장", true),

    // 휴가 관련 (유급)
    ANNUAL_LEAVE("AS101", "연차", true),
    HALF_DAY_AM("AS102", "오전 반차", true),
    HALF_DAY_PM("AS103", "오후 반차", true),
    SICK_LEAVE("AS104", "병가", true),
    MATERNITY_LEAVE("AS105", "출산전후휴가", true),
    PATERNITY_LEAVE("AS106", "배우자 출산휴가", true),
    CHILDCARE_LEAVE("AS107", "육아휴직", true),
    FAMILY_CARE_LEAVE("AS108", "가족돌봄휴가", true),
    MENSTRUAL_LEAVE("AS109", "생리휴가", true),

    // 무급 또는 예외 상태
    ABSENT("AS201", "결근", false),
    UNPAID_LEAVE("AS202", "무급휴가", false);

    private final String codeValue;
    private final String codeName;
    private final boolean isPaid; // 유급 여부 (급여 정산 시 API 응답에 포함됨)

    @JsonValue
    public String getCodeValue() {
        return codeValue;
    }

    @JsonCreator
    public static AttendanceStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown AttendanceStatus: " + code));
    }
}
