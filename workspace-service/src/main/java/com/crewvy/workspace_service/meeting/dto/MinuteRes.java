package com.crewvy.workspace_service.meeting.dto;

import com.crewvy.workspace_service.meeting.entity.Minute;
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
public class MinuteRes {
    private UUID id;
    private String videoConferenceTitle;
    private LocalDateTime actualStartTime;
    private String transcript;
    private String summary;
    private List<UUID> inviteeList;

    public static MinuteRes fromEntity(VideoConference videoConference) {
        Minute minute = videoConference.getRecording().getMinute();
        return MinuteRes.builder()
                .id(minute.getId())
                .transcript(minute.getTranscript())
                .videoConferenceTitle(videoConference.getName())
                .inviteeList(videoConference.getVideoConferenceInviteeSet().stream().map(VideoConferenceInvitee::getId).toList())
                .actualStartTime(videoConference.getActualStartTime())
                .summary(minute.getSummary())
                .build();
    }
}
