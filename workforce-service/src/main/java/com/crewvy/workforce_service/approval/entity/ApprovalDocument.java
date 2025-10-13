package com.crewvy.workforce_service.approval.entity;

import com.crewvy.common.converter.JsonToMapConverter;
import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.approval.dto.request.UpdateDocumentDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class ApprovalDocument extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String documentName;

    @Column(name = "metadata", columnDefinition = "longtext")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> metadata;

    @OneToMany(mappedBy = "approvalDocument", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApprovalPolicy> approvalPolicy;

    public void updateDocument(UpdateDocumentDto dto) {
        this.documentName = dto.getTitle();
        this.metadata = dto.getMetadata();
    }

    public void addApprovalPolicy(ApprovalPolicy policy) {
        this.approvalPolicy.add(policy);
        policy.setApprovalDocument(this); // 연관관계의 양쪽 모두에 값을 설정!
    }
}
