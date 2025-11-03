package com.crewvy.workforce_service.performance.dto.response;

import com.crewvy.workforce_service.performance.constant.TeamGoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TeamGoalDetailResponseDto {
    private String title;
    private String contents;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private UUID memberPositionId;
    @Builder.Default
    private List<GoalResponseDto> goalList = new ArrayList<>();
    @Builder.Default
    private List<TeamGoalMemberResDto> memberList = new ArrayList<>();
}
