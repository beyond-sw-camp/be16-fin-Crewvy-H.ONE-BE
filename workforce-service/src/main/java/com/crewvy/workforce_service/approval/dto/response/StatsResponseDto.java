package com.crewvy.workforce_service.approval.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class StatsResponseDto {
    private int pendingCount;
    private int requestCount;
    private int completeCount;
    private int approveCompleteCount;
    private int draftCount;
}
