package com.crewvy.workforce_service.performance.dto;


import com.crewvy.workforce_service.performance.constant.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GoalResponseDto {
    private UUID goalId;
    private String title;
    private String contents;
    private UUID memberPositionId;
    private LocalDate startDate;
    private LocalDate endDate;
    private GoalStatus status;
    private String teamGoalTitle;
    private String teamGoalContents;
    private Map<String, Object> gradingSystem;
    private List<EvidenceResponseDto> evidenceList;
}
