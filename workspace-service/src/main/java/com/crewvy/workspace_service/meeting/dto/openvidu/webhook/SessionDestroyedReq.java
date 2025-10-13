package com.crewvy.workspace_service.meeting.dto.openvidu.webhook;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionDestroyedReq {
    private String event;
    private Long timestamp;
    private String sessionId;
    private Long startTime;
    private Long duration;
    private String reason;
}
