package com.crewvy.workforce_service.performance.dto;

import com.example.approval.performance.domain.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UpdateStatusDto {
    private UUID goalId;
    private GoalStatus status;
    private String comment;
}
