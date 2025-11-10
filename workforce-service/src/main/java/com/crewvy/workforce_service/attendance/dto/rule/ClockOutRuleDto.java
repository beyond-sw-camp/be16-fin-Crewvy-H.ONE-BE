package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Getter;
import lombok.Setter;

/**
 * 퇴근 정책 규칙
 */
@Getter
@Setter
public class ClockOutRuleDto {
    // 퇴근 중복 허용 여부 (true: 재입력 시 마지막 시각으로 업데이트)
    private Boolean allowDuplicateClockOut;

    // 근무 한도 초과 시 자동 퇴근 처리 여부
    private Boolean autoClockOutEnabled;

    // 최대 근무 시간 (시간) - autoClockOutEnabled가 true일 때 사용
    private Integer autoClockOutAfterHours;

    // 출근 없이 퇴근 허용 여부 (오전 반차 등의 경우)
    private Boolean allowClockOutWithoutClockIn;

    // 퇴근 시간 제한 타입: FIXED_PLUS_HOURS (정규 퇴근 + N시간), END_OF_DAY (당일 자정), WORK_DURATION (출근 기준 + N시간)
    private String limitType;

    // 정규 퇴근 시간 이후 허용 시간 (시간) - limitType이 FIXED_PLUS_HOURS일 때 사용
    private Integer maxHoursAfterWorkEnd;

    // 최대 근무 시간 (시간) - limitType이 WORK_DURATION일 때 사용
    private Integer maxWorkDurationHours;
}
