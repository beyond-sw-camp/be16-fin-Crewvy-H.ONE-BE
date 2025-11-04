package com.crewvy.workforce_service.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyBalanceRes {
    private BalanceTypeInfo balanceTypeCode;
    private Integer year;
    private Double totalGranted;
    private Double totalUsed;
    private Double remaining;
    private LocalDate expirationDate;
    private Boolean isPaid;

    // 분할 사용 관련 정보
    private Integer maxSplitCount;      // 최대 분할 사용 횟수 (정책에서)
    private Integer currentSplitCount;  // 현재 연도 사용 횟수

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceTypeInfo {
        private String codeValue;
        private String codeName;
        private Boolean isBalanceDeductible;
    }
}
