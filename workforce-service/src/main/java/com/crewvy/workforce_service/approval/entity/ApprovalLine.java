package com.crewvy.workforce_service.approval.entity;

import com.crewvy.workforce_service.approval.constant.LineStatus;
import com.crewvy.workforce_service.approval.converter.LineStatusConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
public class ApprovalLine{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private Approval approval;

    private UUID memberPositionId;

    @Convert(converter = LineStatusConverter.class)
    private LineStatus lineStatus;

    private LocalDateTime approvalDate;

    private int lineIndex;

    private String comment;

    public void updateLineStatus(LineStatus status) {
        this.lineStatus = status;
    }

    public void updateLineStatus(LineStatus status, LocalDateTime approvalDate) {
        this.lineStatus = status;
        this.approvalDate = approvalDate;
    }

    public void reject(String comment) {
        this.comment = comment;
    }
}
