package com.crewvy.workforce_service.salary.entity;

import com.crewvy.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class Salary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID memberId;

    private int amount;

    private int netPay;

    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    private SalaryStatus salaryStatus;

    @OneToMany(mappedBy = "ordering", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalaryDetail> salaryDetailList = new ArrayList<>();
}