package com.crewvy.workspace_service.meeting.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageReq {
    private UUID senderId;
    private String name;
    private String content;
}
