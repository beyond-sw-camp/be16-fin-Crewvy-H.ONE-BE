package com.crewvy.workforce_service.salary.dto.request;

import com.crewvy.workforce_service.salary.constant.HolidayRule;
import com.crewvy.workforce_service.salary.constant.PayDayType;
import com.crewvy.workforce_service.salary.constant.PeriodMonthType;
import com.crewvy.workforce_service.salary.constant.PeriodType;
import com.crewvy.workforce_service.salary.entity.SalaryPolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryPolicyUpdateReq {

    private UUID companyId;
    private PayDayType payDayType;
    private int paymentDay;
    private HolidayRule holidayRule;
    private PeriodType periodType;
    private PeriodMonthType periodStartMonthType;
    private int periodStartDay;
    private PeriodMonthType periodEndMonthType;
    private int periodEndDay;

    public SalaryPolicy toEntity() {
        return SalaryPolicy.builder()
                .companyId(this.companyId)
                .payDayType(this.payDayType)
                .paymentDay(this.paymentDay)
                .holidayRule(this.holidayRule)
                .periodType(this.periodType)
                .periodStartMonthType(this.periodStartMonthType)
                .periodStartDay(this.periodStartDay)
                .periodEndMonthType(this.periodEndMonthType)
                .periodEndDay(this.periodEndDay)
                .build();
    }
}
