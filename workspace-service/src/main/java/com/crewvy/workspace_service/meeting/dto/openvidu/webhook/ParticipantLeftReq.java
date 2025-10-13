package com.crewvy.workspace_service.meeting.dto.openvidu.webhook;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantLeftReq {
    private String event;
    private Long timestamp;
    private String sessionId;
    private Long startTime;
    private Long duration;
    private String reason;
    private String connectionId;
    private String location;
    private String ip;
    private String platform;
    private String clientData;
    private String serverData; // json ex) "{'user': 'client1'}"
}
