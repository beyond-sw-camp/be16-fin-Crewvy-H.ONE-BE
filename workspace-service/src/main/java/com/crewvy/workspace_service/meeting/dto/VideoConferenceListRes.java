package com.crewvy.workspace_service.meeting.dto;

import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.meeting.entity.VideoConference;
import com.crewvy.workspace_service.meeting.entity.VideoConferenceInvitee;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoConferenceListRes {
    private UUID id;
    private String name;
    private String description;
    private String scheduledStartTime;
    private String actualStartTime;
    private String status;
    private Boolean isRecording;
    private List<UUID> inviteeIdList;

    public static VideoConferenceListRes fromEntity(VideoConference videoConference) {
        return VideoConferenceListRes.builder()
                .id(videoConference.getId())
                .name(videoConference.getName())
                .description(videoConference.getDescription())
                .scheduledStartTime(videoConference.getScheduledStartTime().toString())
                .actualStartTime(videoConference.getActualStartTime().toString())
                .status(videoConference.getStatus().getCodeName())
                .isRecording(videoConference.getIsRecording().toBoolean())
                .inviteeIdList(videoConference.getVideoConferenceInviteeList().stream().map(VideoConferenceInvitee::getMemberId).toList())
                .build();
    }
}
