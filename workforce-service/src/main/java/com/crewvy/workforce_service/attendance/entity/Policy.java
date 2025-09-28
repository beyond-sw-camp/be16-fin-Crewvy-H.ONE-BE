package com.crewvy.workforce_service.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
public class Policy {

    @Id
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
