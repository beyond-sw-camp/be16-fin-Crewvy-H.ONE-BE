package com.crewvy.workforce_service.salary.dto.response;

import com.crewvy.workforce_service.salary.constant.HolidayRule;
import com.crewvy.workforce_service.salary.constant.PayDayType;
import com.crewvy.workforce_service.salary.constant.PeriodMonthType;
import com.crewvy.workforce_service.salary.constant.PeriodType;
import com.crewvy.workforce_service.salary.entity.SalaryPolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryPolicyRes {

    private PayDayType payDayType;
    private int paymentDay;
    private HolidayRule holidayRule;
    private PeriodType periodType;
    private PeriodMonthType periodStartMonthType;
    private int periodStartDay;
    private PeriodMonthType periodEndMonthType;
    private int periodEndDay;

    public static SalaryPolicyRes fromEntity(SalaryPolicy salaryPolicy) {

        return SalaryPolicyRes.builder()
                .payDayType(salaryPolicy.getPayDayType())
                .paymentDay(salaryPolicy.getPaymentDay())
                .holidayRule(salaryPolicy.getHolidayRule())
                .periodType(salaryPolicy.getPeriodType())
                .periodStartMonthType(salaryPolicy.getPeriodStartMonthType())
                .periodStartDay(salaryPolicy.getPeriodStartDay())
                .periodEndMonthType(salaryPolicy.getPeriodEndMonthType())
                .periodEndDay(salaryPolicy.getPeriodEndDay())
                .build();
    }
}
