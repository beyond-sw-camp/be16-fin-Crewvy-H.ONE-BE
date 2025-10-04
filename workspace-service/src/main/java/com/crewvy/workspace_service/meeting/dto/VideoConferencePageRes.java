package com.crewvy.workspace_service.meeting.dto;

import lombok.*;

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
    private String actualStartTime;
    private Integer participantsCount;
}