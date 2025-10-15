package com.crewvy.workforce_service.performance.dto;

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
    private LocalDate startDate;
    private LocalDate endDate;
    private UUID memberPositionId; // 추후 이름, 직급, 소속팀 정도의 데이터로 대체
}
