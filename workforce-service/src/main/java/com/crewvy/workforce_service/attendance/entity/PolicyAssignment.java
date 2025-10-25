package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.workforce_service.attendance.constant.PolicyScopeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
    @Column(name = "policy_assignment_id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "target_id", nullable = false)
    private UUID targetId; // 할당 대상의 ID (회사, 부서, 직원 등)

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private PolicyScopeType scopeType; // 할당 대상의 종류 (COMPANY, ORGANIZATION, MEMBER)

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public void activate() {
        this.isActive = true;
        this.revokedAt = null;
    }

    public void deactivate() {
        this.isActive = false;
        this.revokedAt = LocalDateTime.now();
    }
}
