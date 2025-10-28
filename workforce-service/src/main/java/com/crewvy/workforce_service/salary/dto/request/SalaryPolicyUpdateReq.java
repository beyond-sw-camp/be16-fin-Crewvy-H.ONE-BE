package com.crewvy.workforce_service.salary.dto.request;

import com.crewvy.workforce_service.salary.constant.HolidayRule;
import com.crewvy.workforce_service.salary.constant.PayDayType;
import com.crewvy.workforce_service.salary.constant.PeriodMonthType;
import com.crewvy.workforce_service.salary.constant.PeriodType;
import lombok.Getter;

import java.util.UUID;

@Getter
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

}
