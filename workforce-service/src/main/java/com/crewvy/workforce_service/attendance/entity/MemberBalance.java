package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.converter.PolicyTypeCodeConverter;
import jakarta.persistence.*;
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
public class MemberBalance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "member_balance_id", nullable = false)
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "balance_type_code", nullable = false)
    @Convert(converter = PolicyTypeCodeConverter.class)
    private PolicyTypeCode balanceTypeCode;

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

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid;

    @Column(name = "is_usable", nullable = false)
    @Builder.Default
    private Boolean isUsable = true; // 사용 가능 여부 (정책 할당 활성 상태와 연동)

    /**
     * 잔액 사용 중단 (정책 할당 비활성화/삭제 시)
     */
    public void suspend() {
        this.isUsable = false;
    }

    /**
     * 잔액 사용 재개 (정책 할당 재활성화 시)
     */
    public void activate() {
        this.isUsable = true;
    }

    /**
     * 추가 일수 부여 (관리자 수동 부여 시)
     */
    public void grantAdditional(Double days) {
        this.totalGranted += days;
        this.remaining += days;
    }

    /**
     * 관리자가 연차 잔액을 직접 수정
     * @param totalGranted 총 부여일수
     * @param totalUsed 총 사용일수
     */
    public void updateBalance(Double totalGranted, Double totalUsed) {
        this.totalGranted = totalGranted;
        this.totalUsed = totalUsed;
        this.remaining = totalGranted - totalUsed;
    }
}
