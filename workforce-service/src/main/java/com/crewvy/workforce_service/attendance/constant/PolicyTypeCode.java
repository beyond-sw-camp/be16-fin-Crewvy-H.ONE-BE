package com.crewvy.workforce_service.attendance.constant;

public enum PolicyTypeCode {
    // 법적으로 필수 휴가
    ANNUAL_LEAVE,          // 연차유급휴가
    MATERNITY_LEAVE,       // 출산전후휴가
    PATERNITY_LEAVE,       // 배우자 출산휴가
    CHILDCARE_LEAVE,       // 육아휴직
    FAMILY_CARE_LEAVE,     // 가족돌봄휴가
    MENSTRUAL_LEAVE,       // 생리휴가

    // 근로시간 관련
    BUSINESS_TRIP,         // 출장
    OVERTIME,              // 연장근무
    NIGHT_WORK,            // 야간근무
    HOLIDAY_WORK;          // 휴일근무
}
