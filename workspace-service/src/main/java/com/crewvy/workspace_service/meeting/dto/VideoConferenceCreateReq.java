package com.crewvy.workspace_service.meeting.dto;

import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.meeting.entity.VideoConference;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoConferenceCreateReq {
    private String name;
    private String description;
    private List<UUID> inviteeIdList = new ArrayList<>();
    private Boolean isRecording;
    private LocalDateTime scheduledStartTime;

    public VideoConference toEntity(UUID hostId) {
        return VideoConference.builder()
                .description(this.description)
                .name(this.name)
                .isRecording(Bool.fromBoolean(this.isRecording))
                .hostId(hostId)
                .scheduledStartTime(this.scheduledStartTime)
                .build();
    }
}
