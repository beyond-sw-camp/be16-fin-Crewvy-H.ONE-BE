package com.crewvy.workforce_service.performance.dto.request;

import com.crewvy.workforce_service.performance.constant.EvaluationType;
import com.crewvy.workforce_service.performance.constant.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CreateEvaluationDto {
    private UUID goalId;
    private Grade grade;
    private EvaluationType type;
    private String comment;
}
