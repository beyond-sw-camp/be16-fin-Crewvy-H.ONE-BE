package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LatenessRuleDto {
    // 지각 허용 시간(분). 이 시간을 넘으면 지각 처리.
    private Integer latenessGraceMinutes;

    // 조퇴 허용 시간(분). 퇴근까지 이 시간 미만으로 남았을 때 퇴근하면 조퇴 처리.
    private Integer earlyLeaveGraceMinutes;

    // 급여 차감 방식: DEDUCT_PER_MINUTE (분 단위 차감), DEDUCT_AS_HALF_DAY (반차로 차감)
    private String deductionType;

    // 월별 지각/조퇴 허용 횟수
    private Integer monthlyAllowedCount;
}
