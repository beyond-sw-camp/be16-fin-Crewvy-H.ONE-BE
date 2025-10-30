package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Data;

/**
 * 근속 연수에 따른 연차 가산 계층을 정의하는 DTO.
 */
@Data
public class LeaveAccrualTierDto {

    /**
     * 적용될 근속 연수 (이상).
     * 예: 3년차부터 적용 시 이 값은 3.
     */
    private Integer yearsOfService;

    /**
     * 해당 근속 연수 계층에 부여될 총 연차 일수.
     */
    private Integer grantDays;
}
