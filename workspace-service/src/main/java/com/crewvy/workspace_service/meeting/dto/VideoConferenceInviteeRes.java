package com.crewvy.workspace_service.meeting.dto;

import com.crewvy.workspace_service.meeting.entity.VideoConferenceInvitee;
import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoConferenceInviteeRes {
    private UUID memberId;
    private String name;

    public static VideoConferenceInviteeRes fromEntity(VideoConferenceInvitee videoConferenceInvitee, String name) {
        return VideoConferenceInviteeRes.builder()
                .memberId(videoConferenceInvitee.getMemberId())
                .name(name)
                .build();
    }
}
