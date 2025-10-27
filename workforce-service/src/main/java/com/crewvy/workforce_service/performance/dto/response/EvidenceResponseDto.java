package com.crewvy.workforce_service.performance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class EvidenceResponseDto {
    private UUID evidenceId;
    private String evidenceUrl;
}
