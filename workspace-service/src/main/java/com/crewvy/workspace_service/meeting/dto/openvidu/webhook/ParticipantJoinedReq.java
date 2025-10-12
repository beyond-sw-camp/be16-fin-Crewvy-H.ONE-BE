package com.crewvy.workspace_service.meeting.dto.openvidu.webhook;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantJoinedReq {
    private String event;
    private Long timestamp;
    private String sessionId;
    private String connectionId;
    private String location;
    private String ip;
    private String platform;
    private String clientData;
    private String serverData; // json ex) "{'user': 'client1'}"
}
