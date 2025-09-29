package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.constant.RequestUnit;
import com.crewvy.workforce_service.attendance.converter.RequestStatusConverter;
import com.crewvy.workforce_service.attendance.converter.RequestUnitConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "request")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "request_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private Policy policy;

    @Column(name = "member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Column(name = "document_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID documentId;

    @Column(name = "request_unit", nullable = false)
    @Convert(converter = RequestUnitConverter.class)
    private RequestUnit requestUnit;

    @Column(name = "start_at", nullable = false)
    private LocalDate startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDate endAt;

    @Column(name = "deduction_days", nullable = false)
    private Double deductionDays;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "status", nullable = false)
    @Convert(converter = RequestStatusConverter.class)
    private RequestStatus status;

    @Column(name = "work_location")
    private String workLocation;

    @Column(name = "requester_comment")
    private String requesterComment;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
