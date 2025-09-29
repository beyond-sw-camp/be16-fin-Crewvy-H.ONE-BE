package com.crewvy.workforce_service.performance.entity;

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

    @Column(nullable = false)
    private UUID memberID;

    @Convert(converter = GradeConverter.class)
    private Grade grade;

    @Convert(converter = EvaluationTypeConverter.class)
    private EvaluationType type;

    private String comment;
}
