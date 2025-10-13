package com.crewvy.workforce_service.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TeamGoalDetailResponseDto {
    private String title;
    private String contents;
    private LocalDate startDate;
    private LocalDate endDate;
    @Builder.Default
    private List<GoalResponseDto> goalList = new ArrayList<>();
}
