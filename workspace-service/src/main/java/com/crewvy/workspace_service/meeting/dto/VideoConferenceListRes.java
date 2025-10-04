package com.crewvy.workspace_service.meeting.dto;

import com.crewvy.workspace_service.meeting.entity.VideoConference;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class VideoConferenceListRes {
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime scheduledStartTime;
    private String status;

    public static VideoConferenceListRes fromEntity(VideoConference videoConference) {
        return VideoConferenceListRes.builder()
                .id(videoConference.getId())
                .name(videoConference.getName())
                .description(videoConference.getDescription())
                .scheduledStartTime(videoConference.getScheduledStartTime())
                .status(videoConference.getStatus().getCodeName())
                .build();
    }
}
