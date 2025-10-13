package com.crewvy.workforce_service.approval.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ApprovalResponseDto {
    private UUID approvalId;
    private String title;
    private Map<String, Object> contents;
    private DocumentResponseDto document;
    private List<ApprovalStepDto> lineList;
    private List<AttachmentResponseDto> attachmentList;
}
