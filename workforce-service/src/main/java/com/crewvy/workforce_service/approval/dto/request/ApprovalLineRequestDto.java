package com.crewvy.workforce_service.approval.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ApprovalLineRequestDto {
    private UUID memberId;
    private int lineIndex;
}
