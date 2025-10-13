package com.crewvy.workforce_service.performance.entity;

import com.crewvy.common.converter.JsonToMapConverter;
import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.performance.constant.GoalStatus;
import com.crewvy.workforce_service.performance.converter.GoalStatusConverter;
import com.crewvy.workforce_service.performance.dto.UpdateMyGoalDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Column(name = "grading_system", columnDefinition = "longtext")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> gradingSystem;

    @Builder.Default
    @OneToMany(mappedBy = "goal", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Evidence> evidenceList = new ArrayList<>();

    public void updateStatus(GoalStatus status, String comment) {
        this.status = status;
        this.comment = comment;
    }

    public void updateGoal(UpdateMyGoalDto dto) {
        this.title = dto.getTitle();
        this.contents = dto.getContents();
        this.startDate = dto.getStartDate();
        this.endDate = dto.getEndDate();
        this.gradingSystem = dto.getGradingSystem();
    }
}
