package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TripRuleDto {
    // 국내, 해외 등 출장 유형
    private String type;
    // 일비 (per diem)
    private BigDecimal perDiemAmount;
    // 숙박비 최대 한도
    private BigDecimal accommodationLimit;
    // 교통비 최대 한도
    private BigDecimal transportationLimit;
}
