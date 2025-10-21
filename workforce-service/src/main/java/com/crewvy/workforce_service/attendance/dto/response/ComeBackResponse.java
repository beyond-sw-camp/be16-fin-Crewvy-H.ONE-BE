package com.crewvy.workforce_service.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ComeBackResponse {
    private UUID eventLogId;
    private LocalDateTime eventTime;
    private Integer totalGoOutMinutes;
}
