package com.crewvy.workforce_service.performance.entity;

import com.crewvy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Builder
public class Goal extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teamgoal_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private TeamGoal teamGoal;

    @Column(nullable = false)
    private UUID memberId;

    private String title;

    private String contents;

    private LocalDate startDate;

    private LocalDate endDate;

    @Convert(converter = GoalStatusConverter.class)
    private GoalStatus status;

    private String comment;
}
