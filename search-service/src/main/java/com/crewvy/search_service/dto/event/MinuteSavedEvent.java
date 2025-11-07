package com.crewvy.search_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinuteSavedEvent {
    private String minuteId;
    private String title;
    private String summary;
    private List<String> memberId;
    private LocalDateTime createDateTime;
}
