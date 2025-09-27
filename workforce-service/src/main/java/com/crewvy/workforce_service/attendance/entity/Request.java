package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.workforce_service.attendance.enums.RequestStatus;
import com.crewvy.workforce_service.attendance.enums.RequestUnit;
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
public class Request {

    @Id
    @Column(name = "request_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID requestId;

    @Column(name = "policy_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID policyId;

    @Column(name = "member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Column(name = "document_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_unit", nullable = false)
    private RequestUnit requestUnit;

    @Column(name = "start_at", nullable = false)
    private LocalDate startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDate endAt;

    @Column(name = "deduction_days", nullable = false)
    private Double deductionDays;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RequestStatus status;

    @Column(name = "work_location")
    private String workLocation;

    @Column(name = "requester_comment")
    private String requesterComment;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
