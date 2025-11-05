package com.crewvy.workspace_service.meeting.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class LiveKitSessionRes {
    private UUID videoConferenceId;
    private String token;
}
