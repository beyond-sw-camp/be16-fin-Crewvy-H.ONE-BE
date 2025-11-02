package com.crewvy.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ScheduleDto {
    private UUID originId;
    private UUID memberId;
    private String title;
    private String contents;
    private String type;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
