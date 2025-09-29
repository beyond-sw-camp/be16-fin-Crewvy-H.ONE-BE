package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "work_schedule")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "work_schedule_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID workScheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private Policy policy;

    @Column(name = "member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Column(name = "date", nullable = false)
    private LocalDate date;
}
