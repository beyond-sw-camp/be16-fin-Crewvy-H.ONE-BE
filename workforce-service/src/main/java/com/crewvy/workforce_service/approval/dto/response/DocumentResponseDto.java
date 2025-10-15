package com.crewvy.workforce_service.approval.dto.response;

import com.crewvy.workforce_service.approval.entity.ApprovalDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DocumentResponseDto {
    private UUID documentId;
    private String documentName;
    private Map<String, Object> metadata;


    public static DocumentResponseDto from(ApprovalDocument approvalDocument) {
        return DocumentResponseDto.builder()
                .documentId(approvalDocument.getId())
                .documentName(approvalDocument.getDocumentName())
                .metadata(approvalDocument.getMetadata())
                .build();
    }
}
