package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.approval.entity.ApprovalDocument;
import com.crewvy.workforce_service.attendance.constant.DeviceType;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.constant.RequestUnit;
import com.crewvy.workforce_service.attendance.converter.DeviceTypeConverter;
import com.crewvy.workforce_service.attendance.converter.RequestStatusConverter;
import com.crewvy.workforce_service.attendance.converter.RequestUnitConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Column(name = "request_id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Policy policy;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    private UUID approvalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ApprovalDocument approvalDocument;

    @Column(name = "request_unit")
    @Convert(converter = RequestUnitConverter.class)
    private RequestUnit requestUnit;

    @Column(name = "start_date_time")
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;

    @Column(name = "deduction_days")
    private Double deductionDays;

    @Column(name = "reason")
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

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "device_type")
    @Convert(converter = DeviceTypeConverter.class)
    private DeviceType deviceType;

    /**
     * 추가근무 신청 시 사용: 1일 연장시간 (분 단위)
     * 여러 날짜에 걸친 추가근무 신청 시 각 날짜마다 적용할 시간
     */
    @Column(name = "daily_overtime_minutes")
    private Integer dailyOvertimeMinutes;

    /**
     * 결재 문서 연결
     * Approval 양식과 연결하기 위해 사용
     */
    public void updateApprovalDocument(ApprovalDocument approvalDocument) {
        this.approvalDocument = approvalDocument;
    }

    /**
     * Request 상태 업데이트
     * Approval 결재 완료 시 APPROVED로 변경
     */
    public void updateStatus(RequestStatus status) {
        this.status = status;
        if (status == RequestStatus.APPROVED || status == RequestStatus.REJECTED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public void updateApprovalId(UUID approvalId) {
        this.approvalId = approvalId;
    }
}
