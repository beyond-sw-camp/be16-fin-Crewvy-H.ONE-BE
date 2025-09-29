package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.attendance.constant.PolicyCategory;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "policy_type")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "policy_type_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID policyTypeId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "type_code", nullable = false)
    private PolicyTypeCode typeCode;

    @Column(name = "type_name", nullable = false)
    private String typeName;

    @Column(name = "balance_deductible", nullable = false)
    private boolean balanceDeductible;

    @Column(name = "category_code", nullable = false)
    private PolicyCategory categoryCode;
}
