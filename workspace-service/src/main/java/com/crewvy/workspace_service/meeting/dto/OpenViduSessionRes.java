package com.crewvy.workspace_service.meeting.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class OpenViduSessionRes {
    private String sessionId;
    private String token;
    private UUID videoConferenceId;
}
