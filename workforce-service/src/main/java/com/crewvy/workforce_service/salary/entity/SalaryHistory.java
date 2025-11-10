package com.crewvy.workforce_service.salary.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.salary.constant.PayType;
import com.crewvy.workforce_service.salary.converter.PayTypeConverter;
import com.crewvy.workforce_service.salary.dto.request.SalaryHistoryCreateReq;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class SalaryHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private int baseSalary;

    @Column(nullable = false)
    private int customaryWage;

    @Column(nullable = false)
    @Convert(converter = PayTypeConverter.class)
    private PayType payType;

    @Column(nullable = false)
    private LocalDate effectiveDate;


    public void update(SalaryHistoryCreateReq salaryHistoryCreateReq, UUID companyId) {
        this.companyId = companyId;
        this.memberId = salaryHistoryCreateReq.getMemberId();
        this.baseSalary = salaryHistoryCreateReq.getBaseSalary();
        this.customaryWage = salaryHistoryCreateReq.getCustomaryWage();
        this.payType = salaryHistoryCreateReq.getPayType();
        this.effectiveDate = salaryHistoryCreateReq.getEffectiveDate();
    }
}
