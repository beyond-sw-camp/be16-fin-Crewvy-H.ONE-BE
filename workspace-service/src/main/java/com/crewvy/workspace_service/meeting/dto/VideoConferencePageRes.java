package com.crewvy.workspace_service.meeting.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoConferencePageRes {
    private UUID id;
    private String sessionId;
    private String name;
    private String description;
    private LocalDateTime actualStartTime;
    private Integer participantsCount;
}