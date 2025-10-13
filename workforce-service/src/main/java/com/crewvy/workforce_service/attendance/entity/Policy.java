package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.attendance.converter.JsonToPolicyRuleDetailsConverter;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "policy")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_type_id")
    private PolicyType policyType;

    @JoinColumn(name = "company_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private UUID companyId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "rule_details", columnDefinition = "json")
    @Convert(converter = JsonToPolicyRuleDetailsConverter.class)
    private PolicyRuleDetails ruleDetails;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    public void update(PolicyType policyType, String name, Boolean isPaid, LocalDate effectiveFrom, LocalDate effectiveTo, PolicyRuleDetails ruleDetails) {
        this.policyType = policyType;
        this.name = name;
        this.isPaid = isPaid;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.ruleDetails = ruleDetails;
    }
}
