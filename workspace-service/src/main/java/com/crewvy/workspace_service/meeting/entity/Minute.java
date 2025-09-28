package com.crewvy.workspace_service.meeting.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workspace_service.meeting.constant.MinuteStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Minute extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "recording_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Recording recording;

    @Column(name = "transcript", nullable = true, columnDefinition = "LONGTEXT")
    private String transcript;

    @Column(name = "summary", nullable = true, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "error_msg", nullable = true, columnDefinition = "TEXT")
    private String errorMsg;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private MinuteStatus status = MinuteStatus.PENDING;
}
