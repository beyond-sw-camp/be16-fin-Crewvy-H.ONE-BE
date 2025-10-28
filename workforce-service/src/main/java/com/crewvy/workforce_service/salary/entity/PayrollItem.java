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

    private UUID companyId;

    @Column(nullable = false)
    @Convert(converter = SalaryTypeConverter.class)
    private SalaryType salaryType;

    @Column(nullable = false)
    private String name;

    private String calculationCode;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Bool isActive = Bool.TRUE;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Bool isTaxable= Bool.FALSE;

    private int nonTaxableLimit;

    private String description;

    @PrePersist // 엔티티가 처음 저장될 때
    @PreUpdate  // 엔티티가 업데이트될 때
    public void validate() {
        if (this.companyId == null && this.calculationCode == null) {
            throw new IllegalStateException("companyId와 calculationCode 중 하나는 반드시 값이 있어야 합니다.");
        }
    }

    public void updateItem(SalaryType salaryType, String name, Bool isActive, String description) {
        this.salaryType = salaryType;
        this.name = name;
        this.isActive = isActive;
        this.description = description;
    }
}
