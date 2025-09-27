package com.crewvy.workforce_service.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "member_balance")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberBalance {

    @Id
    @Column(name = "member_balance_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID memberBalanceId;

    @Column(name = "member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Column(name = "balance_type_code", nullable = false)
    private String balanceTypeCode;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "total_granted", nullable = false)
    private Double totalGranted;

    @Column(name = "total_used", nullable = false)
    private Double totalUsed;

    @Column(name = "remaining", nullable = false)
    private Double remaining;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;
}
