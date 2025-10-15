package com.crewvy.workforce_service.attendance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "policy_assignment")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID policyAssignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "target_id", nullable = false)
    private UUID targetId; // 할당 대상의 ID (회사, 부서, 직원 등)

    @Column(name = "target_type", nullable = false)
    private String targetType; // 할당 대상의 종류 (COMPANY, ORGANIZATION, MEMBER)
}
