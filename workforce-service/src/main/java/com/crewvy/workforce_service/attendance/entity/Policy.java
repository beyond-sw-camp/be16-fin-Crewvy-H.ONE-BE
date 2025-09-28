package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Column(name = "policy_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID policyId;

    @Column(name = "policy_type_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID policyTypeId;

    @Column(name = "company_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID companyId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "rule_details", columnDefinition = "json")
    private String ruleDetails;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
