package com.crewvy.workforce_service.approval.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CreateApprovalDto {
    private UUID documentId;
    private String title;
    private Map<String, Object> contents;
    @Builder.Default
    private List<ApprovalLineRequestDto> lineDtoList = new ArrayList<>();
    private UUID approvalId;
}
