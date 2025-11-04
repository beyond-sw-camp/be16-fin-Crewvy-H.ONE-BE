package com.crewvy.workforce_service.approval.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class RequestResDto {
    private String requestType;
    private String requestUnit;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String reason;
    private String workLocation;
}
