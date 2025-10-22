package com.crewvy.workforce_service.performance.entity;

import com.crewvy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Builder
public class TeamGoal extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID memberPositionId;

    private String title;

    private String contents;

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    @OneToMany(mappedBy = "teamGoal", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamGoalMember> teamGoalMembers  = new ArrayList<>();

    public void updateTeamGoal(String title, String contents, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.contents = contents;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
