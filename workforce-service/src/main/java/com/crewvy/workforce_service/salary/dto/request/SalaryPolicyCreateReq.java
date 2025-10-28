package com.crewvy.workforce_service.salary.dto.request;

import com.crewvy.workforce_service.salary.constant.HolidayRule;
import com.crewvy.workforce_service.salary.constant.PayDayType;
import com.crewvy.workforce_service.salary.constant.PeriodMonthType;
import com.crewvy.workforce_service.salary.constant.PeriodType;
import com.crewvy.workforce_service.salary.entity.SalaryPolicy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalaryPolicyCreateReq {

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
                .companyId(companyId)
                .payDayType(payDayType)
                .paymentDay(paymentDay)
                .holidayRule(holidayRule)
                .periodType(periodType)
                .periodStartMonthType(periodStartMonthType)
                .periodStartDay(periodStartDay)
                .periodEndMonthType(periodEndMonthType)
                .periodEndDay(periodEndDay)
                .build();
    }
}
