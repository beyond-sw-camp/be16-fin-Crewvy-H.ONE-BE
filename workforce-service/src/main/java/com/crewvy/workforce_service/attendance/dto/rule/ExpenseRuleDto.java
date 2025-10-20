package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ExpenseRuleDto {
    // 식대, 교통비, 법인카드 등 경비 유형
    private String type;
    // 1회 사용 한도
    private BigDecimal transactionLimit;
    // 월간 총 한도
    private BigDecimal monthlyLimit;
}
