package com.crewvy.workspace_service.calendar.dto.response;

import com.crewvy.workspace_service.calendar.entity.Calendar;
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
public class CalendarResDto {
    private UUID scheduleId;
    private String title;
    private String typeName;
    private String contents;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public static CalendarResDto from(Calendar calendar) {
        return CalendarResDto.builder()
                .scheduleId(calendar.getId())
                .title(calendar.getTitle())
                .contents(calendar.getContents())
                .typeName(calendar.getType().getCodeName())
                .startDate(calendar.getStartDate())
                .endDate(calendar.getEndDate())
                .build();
    }
}
