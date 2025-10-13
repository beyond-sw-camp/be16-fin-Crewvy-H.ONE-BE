package com.crewvy.workspace_service.meeting.dto;

import com.crewvy.workspace_service.meeting.entity.VideoConference;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoConferenceBookRes {
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime scheduledStartTime;

    public static VideoConferenceBookRes fromEntity(VideoConference videoConference) {
        return VideoConferenceBookRes.builder()
                .id(videoConference.getId())
                .name(videoConference.getName())
                .description(videoConference.getDescription())
                .scheduledStartTime(videoConference.getScheduledStartTime())
                .build();
    }
}