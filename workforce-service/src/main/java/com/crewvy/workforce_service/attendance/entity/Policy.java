package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.converter.JsonToPolicyRuleDetailsConverter;
import com.crewvy.workforce_service.attendance.converter.PolicyTypeCodeConverter;
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
    private UUID id;

    @Column(name = "policy_type_code", nullable = false)
    @Convert(converter = PolicyTypeCodeConverter.class)
    private PolicyTypeCode policyTypeCode;

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

    @Column(name = "is_active")
    private Boolean isActive;

    /**
     * 자동 승인 여부.
     * true일 경우, 이 정책으로 신청한 요청이 결재 없이 자동으로 승인됩니다.
     * false일 경우, 일반적인 결재 프로세스를 따릅니다.
     * 주로 연장근무, 야간근무, 휴일근무 등에 사용됩니다.
     */
    @Column(name = "auto_approve")
    private Boolean autoApprove;

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void update(PolicyTypeCode policyTypeCode, String name, Boolean isPaid, LocalDate effectiveFrom, LocalDate effectiveTo, PolicyRuleDetails ruleDetails, Boolean autoApprove) {
        this.policyTypeCode = policyTypeCode;
        this.name = name;
        this.isPaid = isPaid;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.ruleDetails = ruleDetails;
        this.autoApprove = autoApprove;
    }
}
