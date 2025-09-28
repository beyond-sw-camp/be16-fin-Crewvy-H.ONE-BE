package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.entity.BaseEntity;
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
    @Column(name = "policy_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID policyId;

    @Column(name = "policy_type_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID policyTypeId;

    @Column(name = "company_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID companyId;

    @Column(name = "name", nullable = false)
    private String name;

    // 정책별 세부 규칙 (JSON 형식으로 저장)
    @Column(name = "rule_details", columnDefinition = "json")
    private String ruleDetails;

    // 유급 여부
    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid;

    // 정책 유효 시작일
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    // 정책 유효 종료일 (null이면 현재도 유효)
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}
