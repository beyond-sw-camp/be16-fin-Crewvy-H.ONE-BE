package com.crewvy.workforce_service.salary.entity;


import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class SalaryDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private Salary salary;

    @Enumerated(EnumType.STRING)
    private SalaryType salaryType;

}
