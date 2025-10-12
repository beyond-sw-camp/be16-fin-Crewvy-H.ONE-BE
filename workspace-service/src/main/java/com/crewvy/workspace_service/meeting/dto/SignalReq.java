package com.crewvy.workspace_service.meeting.dto;

import io.openvidu.java.client.Connection;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SignalReq {
    private String data;
    private String type;
    private List<Connection> to;
}
