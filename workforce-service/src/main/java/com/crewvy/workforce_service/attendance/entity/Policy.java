package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.converter.JsonToMapConverter;
import com.crewvy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_type_id")
    private PolicyType policyType;

    @Column(name = "company_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID companyId;

    @Column(name = "name", nullable = false)
    private String name;

    // 정책별 세부 규칙 (JSON 형식으로 저장)
    @Column(name = "rule_details", columnDefinition = "json")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> ruleDetails;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}
