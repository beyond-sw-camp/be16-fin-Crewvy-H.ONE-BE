package com.crewvy.workforce_service.approval.dto.response;

import com.crewvy.workforce_service.approval.constant.RequirementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PolicyResDto {
    private RequirementType requirementType;
    private UUID requirementId;
    private String name;
    private int lineIndex;
}
