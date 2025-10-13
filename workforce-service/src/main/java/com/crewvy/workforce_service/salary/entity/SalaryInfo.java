package com.crewvy.workforce_service.salary.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.salary.dto.request.SalaryInfoCreateReq;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class SalaryInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_item_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private PayrollItem payrollItem;

    private BigInteger amount;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    public void updateSalaryInfo(UUID companyId, PayrollItem payrollItem, BigInteger amount, LocalDateTime startDate, LocalDateTime endDate) {
        this.companyId = companyId;
        this.payrollItem = payrollItem;
        this.amount = amount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static SalaryInfo from(SalaryInfoCreateReq req, PayrollItem payrollItem) {
        return SalaryInfo.builder()
                .companyId(req.getCompanyId())
                .memberId(req.getMemberId())
                .payrollItem(payrollItem)
                .amount(req.getAmount())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .build();
    }
}
