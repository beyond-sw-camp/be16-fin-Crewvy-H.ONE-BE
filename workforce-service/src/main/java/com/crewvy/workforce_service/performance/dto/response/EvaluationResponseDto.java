package com.crewvy.workforce_service.performance.dto.response;

import com.crewvy.workforce_service.performance.constant.EvaluationType;
import com.crewvy.workforce_service.performance.constant.Grade;
import com.crewvy.workforce_service.performance.entity.Evaluation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class EvaluationResponseDto {
    private UUID evaluationId;
    private Grade grade;
    private EvaluationType type;
    private String comment;

    public static EvaluationResponseDto from(Evaluation evaluation) {
        return EvaluationResponseDto.builder()
                .evaluationId(evaluation.getId())
                .grade(evaluation.getGrade())
                .type(evaluation.getType())
                .comment(evaluation.getComment())
                .build();
    }
}
