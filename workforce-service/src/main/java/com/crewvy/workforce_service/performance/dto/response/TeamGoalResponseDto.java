package com.crewvy.workforce_service.performance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TeamGoalResponseDto {
    private UUID teamGoalId;
    private String title;
    private String contents;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private UUID memberPositionId;
    private String memberName;
    private String memberOrganization;
    private String memberPosition;
}
