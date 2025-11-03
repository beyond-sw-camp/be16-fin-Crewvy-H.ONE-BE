package com.crewvy.workforce_service.salary.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.salary.constant.HolidayRule;
import com.crewvy.workforce_service.salary.constant.PayDayType;
import com.crewvy.workforce_service.salary.constant.PeriodMonthType;
import com.crewvy.workforce_service.salary.constant.PeriodType;
import com.crewvy.workforce_service.salary.converter.HolidayRuleConverter;
import com.crewvy.workforce_service.salary.converter.PayDayTypeConverter;
import com.crewvy.workforce_service.salary.converter.PeriodMonthTypeConverter;
import com.crewvy.workforce_service.salary.converter.PeriodTypeConverter;
import com.crewvy.workforce_service.salary.dto.request.SalaryPolicyUpdateReq;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class SalaryPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    @Convert(converter = PayDayTypeConverter.class)
    private PayDayType payDayType;

    private int paymentDay;

    @Column(nullable = false)
    @Convert(converter = HolidayRuleConverter.class)
    private HolidayRule holidayRule;

    @Column(nullable = false)
    @Convert(converter = PeriodTypeConverter.class)
    private PeriodType periodType;

    @Column(nullable = false)
    @Convert(converter = PeriodMonthTypeConverter.class)
    private PeriodMonthType periodStartMonthType;

    @Column(nullable = false)
    private int periodStartDay;

    @Column(nullable = false)
    @Convert(converter = PeriodMonthTypeConverter.class)
    private PeriodMonthType periodEndMonthType;

    private int periodEndDay;

    public void update(SalaryPolicyUpdateReq salaryPolicyUpdateReq) {

        if (salaryPolicyUpdateReq.getPayDayType() != null) {
            this.payDayType = salaryPolicyUpdateReq.getPayDayType();
        }
        if (salaryPolicyUpdateReq.getPaymentDay() != 0) {
            this.paymentDay = salaryPolicyUpdateReq.getPaymentDay();
        }
        if (salaryPolicyUpdateReq.getHolidayRule() != null) {
            this.holidayRule = salaryPolicyUpdateReq.getHolidayRule();
        }
        if (salaryPolicyUpdateReq.getPeriodStartMonthType() != null) {
            this.periodStartMonthType = salaryPolicyUpdateReq.getPeriodStartMonthType();
        }
        if (salaryPolicyUpdateReq.getPeriodStartDay() != 0) {
            this.periodStartDay = salaryPolicyUpdateReq.getPeriodStartDay();
        }
        if (salaryPolicyUpdateReq.getPeriodType() != null) {
            this.periodType = salaryPolicyUpdateReq.getPeriodType();
        }
        if (salaryPolicyUpdateReq.getPeriodEndMonthType() != null) {
            this.periodEndMonthType = salaryPolicyUpdateReq.getPeriodEndMonthType();
        }
        if (salaryPolicyUpdateReq.getPeriodEndDay() != 0) {
            this.periodEndDay = salaryPolicyUpdateReq.getPeriodEndDay();
        }

    }

}
