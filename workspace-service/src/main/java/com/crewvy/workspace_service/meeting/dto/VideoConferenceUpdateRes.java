package com.crewvy.workspace_service.meeting.dto;

import com.crewvy.workspace_service.meeting.entity.VideoConference;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoConferenceUpdateRes {
    private UUID id;
    private String name;
    private String description;
    private Boolean isRecording;
    private LocalDateTime scheduledStartTime;
//    private List<UUID> inviteeIdList;

    public static VideoConferenceUpdateRes fromEntity(VideoConference videoConference) {
        return VideoConferenceUpdateRes.builder()
                .id(videoConference.getId())
                .name(videoConference.getName())
                .description(videoConference.getDescription())
                .scheduledStartTime(videoConference.getScheduledStartTime())
                .isRecording(videoConference.getIsRecording().toBoolean())
//                .inviteeIdList(videoConference.getVideoConferenceInviteeList().stream()
//                        .map(VideoConferenceInvitee::getMemberId)
//                        .toList())
                .build();
    }
}
