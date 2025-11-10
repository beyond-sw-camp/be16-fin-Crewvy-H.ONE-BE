package com.crewvy.workforce_service.performance.entity;

import com.crewvy.workforce_service.performance.constant.EvaluationType;
import com.crewvy.workforce_service.performance.constant.Grade;
import com.crewvy.workforce_service.performance.converter.EvaluationTypeConverter;
import com.crewvy.workforce_service.performance.converter.GradeConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Builder
public class Evaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private Goal goal;

    private UUID memberPositionId;

    @Convert(converter = GradeConverter.class)
    private Grade grade;

    @Convert(converter = EvaluationTypeConverter.class)
    private EvaluationType type;

    private String comment;
}
