package com.crewvy.workforce_service.salary.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.salary.constant.SalaryStatus;
import com.crewvy.workforce_service.salary.converter.SalaryStatusConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class Salary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private BigInteger totalAllowance;

    @Column(nullable = false)
    private BigInteger totalDeduction;

    @Column(nullable = false)
    private BigInteger netPay;

    private LocalDate paymentDate;

    @Column(nullable = false)
    @Convert(converter = SalaryStatusConverter.class)
    private SalaryStatus salaryStatus;

    @Builder.Default
    @OneToMany(mappedBy = "salary", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalaryDetail> salaryDetailList = new ArrayList<>();

    // 급여 정보 수정 메서드
    public void updateSalary(BigInteger totalAllowance,
                             BigInteger totalDeduction,
                             BigInteger netPay,
                             LocalDate paymentDate) {
        this.totalAllowance = totalAllowance;
        this.totalDeduction = totalDeduction;
        this.netPay = netPay;
        if (paymentDate != null) {
            this.paymentDate = paymentDate;
        }
    }

    public void updateSalaryStatus(SalaryStatus salaryStatus) {
        this.salaryStatus = salaryStatus;
    }
}