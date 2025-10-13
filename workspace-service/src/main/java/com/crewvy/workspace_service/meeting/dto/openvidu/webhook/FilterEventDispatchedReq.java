package com.crewvy.workspace_service.meeting.dto.openvidu.webhook;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FilterEventDispatchedReq {
    private String event;
    private Long timestamp;
    private String sessionId;
    private String connectionId;
    private String streamId;
    private String filterType;
    private String eventType;
    private String data; // json "{timestampMillis=1568645808285, codeType=EAN-13, source=23353-1d3c_kurento.MediaPipeline/1f56f4a5-807c-71a30d40_kurento.ZBarFilter, type=CodeFound, value=0012345678905, tags=[], timestamp=1568645808}"
}
