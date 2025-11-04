package com.crewvy.workspace_service.meeting.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoConferencePasswordRes {
    private String id;
    private String password;
}
