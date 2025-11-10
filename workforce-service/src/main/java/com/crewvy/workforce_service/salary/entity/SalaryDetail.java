package com.crewvy.workforce_service.salary.entity;


import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.converter.SalaryTypeConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class SalaryDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private Salary salary;

    @Column(nullable = false)
    @Convert(converter = SalaryTypeConverter.class)
    private SalaryType salaryType;

    @Column(nullable = false)
    private String salaryName;

    @Column(nullable = false)
    private BigInteger amount;
}
