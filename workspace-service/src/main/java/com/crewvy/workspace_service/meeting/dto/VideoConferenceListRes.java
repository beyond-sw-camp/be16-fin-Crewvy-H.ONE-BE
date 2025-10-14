package com.crewvy.workspace_service.meeting.dto;

import com.crewvy.workspace_service.meeting.entity.VideoConference;
import com.crewvy.workspace_service.meeting.entity.VideoConferenceInvitee;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

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
    private LocalDateTime scheduledStartTime;
    private LocalDateTime actualStartTime;
    private String status;
    private Boolean isRecording;
    private List<UUID> inviteeIdList;

    public static VideoConferenceListRes fromEntity(VideoConference videoConference) {
        return VideoConferenceListRes.builder()
                .id(videoConference.getId())
                .name(videoConference.getName())
                .description(videoConference.getDescription())
                .scheduledStartTime(videoConference.getScheduledStartTime())
                .actualStartTime(videoConference.getActualStartTime())
                .status(videoConference.getStatus().getCodeName())
                .isRecording(videoConference.getIsRecording().toBoolean())
                .inviteeIdList(videoConference.getVideoConferenceInviteeSet().stream().map(VideoConferenceInvitee::getMemberId).toList())
                .build();
    }
}
