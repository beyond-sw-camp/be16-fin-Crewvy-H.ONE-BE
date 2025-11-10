package com.crewvy.workforce_service.approval.dto.request;

import com.crewvy.workforce_service.approval.constant.RequirementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DocumentPolicyDto {
    private RequirementType requirementType;
    private UUID requirementId;
    private int lineIndex;
}
