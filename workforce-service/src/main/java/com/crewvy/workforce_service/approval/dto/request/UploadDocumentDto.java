package com.crewvy.workforce_service.approval.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UploadDocumentDto {
    private String documentName;
    private Map<String, Object> metadata;
}
