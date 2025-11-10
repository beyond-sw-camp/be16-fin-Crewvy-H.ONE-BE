package com.crewvy.workforce_service.performance.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UpdateMyGoalDto {
    private UUID goalId;
    private String title;
    private String contents;
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, Object> gradingSystem;
}
