package com.crewvy.workspace_service.meeting.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class LiveKitSessionRes {
    private UUID videoConferenceId;
    private String token;
}
