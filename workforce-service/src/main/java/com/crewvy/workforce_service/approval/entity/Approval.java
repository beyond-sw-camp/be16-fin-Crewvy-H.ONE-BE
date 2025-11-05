package com.crewvy.workforce_service.approval.entity;

import com.crewvy.common.converter.JsonToMapConverter;
import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.approval.constant.ApprovalState;
import com.crewvy.workforce_service.approval.converter.ApprovalStateConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class Approval extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private ApprovalDocument approvalDocument;

    private UUID memberPositionId;

    private String title;

    @Column(name = "contents", columnDefinition = "longtext")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> contents;

    @Convert(converter = ApprovalStateConverter.class)
    private ApprovalState state;

    @Builder.Default
    @OneToMany(mappedBy = "approval", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApprovalLine> approvalLineList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "approval", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachmentList = new ArrayList<>();

    public void updateState(ApprovalState state) {
        this.state = state;
    }

    public void updateApproval(String title, Map<String, Object> contents) {
        this.title = title;
        this.contents = contents;
    }
}