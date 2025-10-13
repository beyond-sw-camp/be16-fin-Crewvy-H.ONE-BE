package com.crewvy.workforce_service.salary.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.common.entity.Bool;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.converter.SalaryTypeConverter;
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
public class PayrollItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    @Convert(converter = SalaryTypeConverter.class)
    private SalaryType salaryType;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Bool isActive = Bool.TRUE;

    private String description;

    public void updateItem(SalaryType salaryType, String name, Bool isActive, String description) {
        this.salaryType = salaryType;
        this.name = name;
        this.isActive = isActive;
        this.description = description;
    }
}
