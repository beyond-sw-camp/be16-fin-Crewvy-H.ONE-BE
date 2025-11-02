package com.crewvy.workspace_service.calendar.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PersonalScheduleReqDto {
    private String title;
    private String contents;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
