package com.crewvy.workspace_service.meeting.dto;

import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.meeting.entity.VideoConference;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoConferenceCreateReq {
    private String name;
    private String description;
    private List<UUID> inviteeIdList;
    private Boolean isRecording;
    private String scheduledStartTime;

    public VideoConference toEntity(UUID hostId) {
        LocalDateTime scheduledStartTime = this.scheduledStartTime == null ? null : LocalDateTime.parse(this.scheduledStartTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        return VideoConference.builder()
                .description(this.description)
                .name(this.name)
                .isRecording(this.isRecording ? Bool.TRUE : Bool.FALSE)
                .hostId(hostId)
                .scheduledStartTime(scheduledStartTime)
                .build();
    }
}
