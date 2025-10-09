package com.crewvy.workforce_service.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CreateTeamGoalDto {
    private String title;
    private String contents;
    private LocalDate startDate;
    private LocalDate endDate;
}
