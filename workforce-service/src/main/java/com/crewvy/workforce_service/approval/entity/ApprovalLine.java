package com.crewvy.workforce_service.approval.entity;

import com.crewvy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
public class ApprovalLine{
    @Id
    @UuidGenerator
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private Approval approval;

    @Column(nullable = false)
    private UUID memberId;

    @Convert(converter = LineStatusConverter.class)
    private LineStatus lineStatus;

    @Column(nullable = false)
    private LocalDateTime approvalDate;

    @Column(nullable = false)
    private int lineIndex;
}
