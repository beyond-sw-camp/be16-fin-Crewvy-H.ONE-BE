package com.crewvy.workforce_service.performance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class EvaluationStatResDto {
    private int myGoalCount;
    private int teamGoalCount;
    private int myGoalCompleteCount;
    private int teamGoalCompleteCount;
}
