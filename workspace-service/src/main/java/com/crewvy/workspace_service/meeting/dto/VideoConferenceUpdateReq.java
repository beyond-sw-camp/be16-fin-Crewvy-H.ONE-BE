package com.crewvy.workspace_service.meeting.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoConferenceUpdateReq {
    private String name;
    private String description;
    private String scheduledStartTime;
    private Boolean isRecording;
    private List<UUID> inviteeIdList = new ArrayList<>();
}
