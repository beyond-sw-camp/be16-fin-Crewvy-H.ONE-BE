package com.crewvy.workforce_service.approval.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.approval.constant.RequirementType;
import com.crewvy.workforce_service.approval.converter.RequirementTypeConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
public class ApprovalPolicy extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private ApprovalDocument approvalDocument;

    private UUID companyId;

    @Convert(converter = RequirementTypeConverter.class)
    private RequirementType requirementType;

    private UUID requirementId;

    private int lineIndex;
}
